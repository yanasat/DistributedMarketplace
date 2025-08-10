package marketplace;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.zeromq.ZMQ;

import messaging.MessageUtils;
import model.Order;
import model.Order.Status;

public class Marketplace {
    private final List<String> sellerEndpoints;
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    private final int timeoutMs;
    private final String marketplaceId;

    public Marketplace(List<String> sellerEndpoints, String marketplaceId) {
        this(sellerEndpoints, 2000, marketplaceId);
    }

    public Marketplace(List<String> sellerEndpoints, int timeoutMs, String marketplaceId) {
        this.sellerEndpoints = sellerEndpoints;
        this.timeoutMs = timeoutMs;
        this.marketplaceId = marketplaceId != null ? marketplaceId : "MP-" + System.currentTimeMillis();
    }

    // Backward compatibility
    public Marketplace(List<String> sellerEndpoints) {
        this(sellerEndpoints, 2000, "MP-DEFAULT");
    }

    public Marketplace(List<String> sellerEndpoints, int timeoutMs) {
        this(sellerEndpoints, timeoutMs, "MP-DEFAULT");
    }

    /**
     * KORREKTE SAGA-Implementierung mit "ALLES-ODER-NICHTS" Semantik
     * Kunde bekommt entweder die KOMPLETTE Bestellung oder gar nichts
     */
    public void placeOrder(String product, int quantity) {
        Order order = new Order(product, quantity, marketplaceId);
        System.out.println("=== Starting SAGA transaction for order: " + order.getId() + " ===");
        System.out.println("    Marketplace: " + marketplaceId);
        System.out.println("    Product: " + product + ", Quantity: " + quantity);
        System.out.println("    SAGA Rule: Customer gets ALL " + quantity + " items or NONE");

        long sagaStartTime = System.currentTimeMillis();

        // Phase 1: RESERVE - Send reservation requests to all sellers
        List<Future<ReserveResult>> futures = sellerEndpoints.stream()
                .map(endpoint -> executor.submit(() -> reserve(endpoint, order)))
                .collect(Collectors.toList());

        // Collect responses with timeout
        for (int i = 0; i < futures.size(); i++) {
            try {
                ReserveResult result = futures.get(i).get(timeoutMs, TimeUnit.MILLISECONDS);
                String endpoint = sellerEndpoints.get(i);
                
                if (result.success) {
                    order.setStatus(endpoint, Status.CONFIRMED);
                    System.out.println("✅ Seller " + endpoint + " CONFIRMED reservation");
                } else {
                    order.setStatus(endpoint, Status.REJECTED);
                    System.out.println("❌ Seller " + endpoint + " REJECTED reservation: " + result.reason);
                }
            } catch (Exception e) {
                String endpoint = sellerEndpoints.get(i);
                System.out.println("⏰ Timeout/Error for seller " + endpoint + ": " + e.getMessage());
                order.setStatus(endpoint, Status.REJECTED);
            }
        }

        // Phase 2: KORREKTE SAGA-Entscheidung - "ALLES-ODER-NICHTS"
        long confirmedCount = order.getSellerStatus().values().stream()
                .mapToLong(status -> status == Status.CONFIRMED ? 1 : 0)
                .sum();

        // KRITISCHE ÄNDERUNG: Nur COMMIT wenn ALLE benötigten Items verfügbar sind
        if (confirmedCount >= quantity) {
            System.out.println("🎉 SAGA SUCCESS: " + confirmedCount + " seller(s) confirmed, " + 
                             quantity + " needed. Customer gets ALL " + quantity + " items!");
            System.out.println("📝 Proceeding with ATOMIC COMMIT...");
            commitCompleteOrder(order, quantity);
        } else {
            System.out.println("❌ SAGA FAILURE: Only " + confirmedCount + " seller(s) confirmed, but " + 
                             quantity + " needed. Customer gets NOTHING!");
            System.out.println("🔄 Proceeding with ATOMIC ROLLBACK...");
            rollbackCompleteOrder(order);
        }
        
        long totalTime = System.currentTimeMillis() - sagaStartTime;
        System.out.println("=== SAGA transaction completed for order: " + order.getId() + 
                         " (total time: " + totalTime + "ms) ===\n");
    }

    /**
     * Reservierung bei einem einzelnen Seller
     */
    private ReserveResult reserve(String endpoint, Order order) {
        try (ZMQ.Socket socket = MessageUtils.createSocket("REQ", false, endpoint)) {
            socket.setReceiveTimeOut(timeoutMs);
            socket.setSendTimeOut(1000);
            
            String msg = String.format("RESERVE:%s:%s:%d", order.getId(), order.getProduct(), 1); // Jeder Seller reserviert 1 Stück
            
            long startTime = System.currentTimeMillis();
            socket.send(msg);
            
            String reply = socket.recvStr();
            long responseTime = System.currentTimeMillis() - startTime;
            
            if (reply != null) {
                System.out.println("RESERVE response from " + endpoint + ": " + reply + 
                                 " (took " + responseTime + "ms)");
                
                // ROBUST PARSING - handle corrupted messages
                try {
                    String cleanReply = reply.replaceAll("[^\\p{Print}]", "").trim();
                    
                    if (cleanReply.startsWith("CONFIRMED")) {
                        return new ReserveResult(true, "Confirmed");
                    } else if (cleanReply.startsWith("REJECTED")) {
                        return new ReserveResult(false, "Rejected by seller");
                    } else if (cleanReply.isEmpty() || cleanReply.length() < 3) {
                        System.out.println("⚠️ CORRUPTED MESSAGE from " + endpoint + 
                                         ": Raw bytes: " + java.util.Arrays.toString(reply.getBytes()));
                        return new ReserveResult(false, "Corrupted message received");
                    } else {
                        return new ReserveResult(false, "Unexpected response: " + cleanReply);
                    }
                } catch (Exception parseError) {
                    System.out.println("❌ PARSE ERROR for response from " + endpoint + 
                                     ": " + parseError.getMessage());
                    return new ReserveResult(false, "Parse error: " + parseError.getMessage());
                }
            } else {
                return new ReserveResult(false, "No response (timeout)");
            }
        } catch (Exception e) {
            return new ReserveResult(false, "Communication error: " + e.getMessage());
        }
    }

    /**
     * ATOMIC COMMIT: Committet nur die benötigte Anzahl von Sellern
     * Überschüssige Reservierungen werden zurückgegeben
     */
    private void commitCompleteOrder(Order order, int neededQuantity) {
        System.out.println("📝 Starting ATOMIC COMMIT phase for " + order.getId());
        System.out.println("    Committing exactly " + neededQuantity + " items");
        
        int committed = 0;
        
        for (String endpoint : sellerEndpoints) {
            Status status = order.getStatus(endpoint);
            
            if (status == Status.CONFIRMED && committed < neededQuantity) {
                // Committen - dieser Seller wird verwendet
                commit(endpoint, order);
                committed++;
                System.out.println("    ✅ COMMITTED item " + committed + "/" + neededQuantity + 
                                 " from " + endpoint);
            } else if (status == Status.CONFIRMED && committed >= neededQuantity) {
                // Überschüssige Reservierung freigeben
                rollback(endpoint, order);
                System.out.println("    🔄 RELEASED surplus reservation from " + endpoint);
            }
        }
        
        System.out.println("💚 ATOMIC COMMIT SUCCESSFUL: Customer receives " + committed + 
                         " items as ordered!");
    }

    /**
     * ATOMIC ROLLBACK: Alle Reservierungen werden rückgängig gemacht
     */
    private void rollbackCompleteOrder(Order order) {
        System.out.println("↩️ Starting ATOMIC ROLLBACK phase for " + order.getId());
        System.out.println("    Rolling back ALL reservations");
        
        int rolledBack = 0;
        
        for (String endpoint : sellerEndpoints) {
            Status status = order.getStatus(endpoint);
            if (status == Status.CONFIRMED) {
                rollback(endpoint, order);
                rolledBack++;
                System.out.println("    🔄 ROLLED BACK reservation " + rolledBack + 
                                 " from " + endpoint);
            }
        }
        
        System.out.println("💔 ATOMIC ROLLBACK COMPLETE: Customer receives NOTHING (as per SAGA rules)");
    }

    /**
     * Einzelnen Seller committen
     */
    private void commit(String endpoint, Order order) {
        try (ZMQ.Socket socket = MessageUtils.createSocket("REQ", false, endpoint)) {
            socket.setReceiveTimeOut(timeoutMs);
            socket.setSendTimeOut(1000);
            
            String msg = String.format("COMMIT:%s:%s:%d", order.getId(), order.getProduct(), 1);
            
            long startTime = System.currentTimeMillis();
            socket.send(msg);
            
            String reply = socket.recvStr();
            long responseTime = System.currentTimeMillis() - startTime;
            
            if (reply != null) {
                System.out.println("COMMIT response from " + endpoint + ": " + reply + 
                                 " (took " + responseTime + "ms)");
            } else {
                System.out.println("⚠️ No COMMIT response from " + endpoint + " (timeout)");
            }
        } catch (Exception e) {
            System.out.println("❌ Error during COMMIT to " + endpoint + ": " + e.getMessage());
        }
    }

    /**
     * Einzelnen Seller rollback
     */
    private void rollback(String endpoint, Order order) {
        try (ZMQ.Socket socket = MessageUtils.createSocket("REQ", false, endpoint)) {
            socket.setReceiveTimeOut(timeoutMs);
            socket.setSendTimeOut(1000);
            
            String msg = String.format("CANCEL:%s:%s:%d", order.getId(), order.getProduct(), 1);
            
            long startTime = System.currentTimeMillis();
            socket.send(msg);
            
            String reply = socket.recvStr();
            long responseTime = System.currentTimeMillis() - startTime;
            
            if (reply != null) {
                System.out.println("ROLLBACK response from " + endpoint + ": " + reply + 
                                 " (took " + responseTime + "ms)");
            } else {
                System.out.println("⚠️ No ROLLBACK response from " + endpoint + " (timeout)");
            }
        } catch (Exception e) {
            System.out.println("❌ Error during ROLLBACK to " + endpoint + ": " + e.getMessage());
        }
    }

    /**
     * Clean shutdown
     */
    public void stop() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Helper class for reserve results
     */
    private static class ReserveResult {
        final boolean success;
        final String reason;

        ReserveResult(boolean success, String reason) {
            this.success = success;
            this.reason = reason;
        }
    }
}