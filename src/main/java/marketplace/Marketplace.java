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

    public void placeOrder(String product, int quantity) {
        Order order = new Order(product, quantity, marketplaceId);
        System.out.println("=== Starting SAGA transaction for order: " + order.getId() + " ===");
        System.out.println("    Marketplace: " + marketplaceId);
        System.out.println("    Product: " + product + ", Quantity: " + quantity);

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

        // Phase 2: Decision - COMMIT or ROLLBACK
        long decisionTime = System.currentTimeMillis();
        
        if (order.isFullyConfirmed()) {
            System.out.println("🎉 Order CONFIRMED by all sellers. Sending COMMIT...");
            commitOrder(order);
        } else {
            System.out.println("🔄 One or more sellers REJECTED. Rolling back...");
            rollbackOrder(order);
        }
        
        long totalTime = System.currentTimeMillis() - sagaStartTime;
        System.out.println("=== SAGA transaction completed for order: " + order.getId() + 
                         " (total time: " + totalTime + "ms) ===\n");
    }

    private ReserveResult reserve(String endpoint, Order order) {
        try (ZMQ.Socket socket = MessageUtils.createSocket("REQ", false, endpoint)) {
            socket.setReceiveTimeOut(timeoutMs);
            socket.setSendTimeOut(1000);
            
            String msg = String.format("RESERVE:%s:%s:%d", order.getId(), order.getProduct(), order.getQuantity());
            
            long startTime = System.currentTimeMillis();
            socket.send(msg);
            
            String reply = socket.recvStr();
            long responseTime = System.currentTimeMillis() - startTime;
            
            if (reply != null) {
                System.out.println("RESERVE response from " + endpoint + ": " + reply + 
                                 " (took " + responseTime + "ms)");
                
                if (reply.startsWith("CONFIRMED")) {
                    return new ReserveResult(true, "Confirmed");
                } else if (reply.startsWith("REJECTED")) {
                    return new ReserveResult(false, "Rejected by seller");
                } else {
                    return new ReserveResult(false, "Unexpected response: " + reply);
                }
            } else {
                return new ReserveResult(false, "No response (timeout)");
            }
        } catch (Exception e) {
            return new ReserveResult(false, "Communication error: " + e.getMessage());
        }
    }

    private void commitOrder(Order order) {
        System.out.println("📝 Starting COMMIT phase for " + order.getId());
        order.getSellerStatus().forEach((endpoint, status) -> {
            if (status == Status.CONFIRMED) {
                commit(endpoint, order);
            }
        });
    }

    private void commit(String endpoint, Order order) {
        try (ZMQ.Socket socket = MessageUtils.createSocket("REQ", false, endpoint)) {
            socket.setReceiveTimeOut(timeoutMs);
            socket.setSendTimeOut(1000);
            
            String msg = String.format("COMMIT:%s:%s:%d", order.getId(), order.getProduct(), order.getQuantity());
            
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

    private void rollbackOrder(Order order) {
        System.out.println("↩️ Starting ROLLBACK phase for " + order.getId());
        order.getSellerStatus().forEach((endpoint, status) -> {
            if (status == Status.CONFIRMED) {
                rollback(endpoint, order);
            }
        });
    }

    private void rollback(String endpoint, Order order) {
        try (ZMQ.Socket socket = MessageUtils.createSocket("REQ", false, endpoint)) {
            socket.setReceiveTimeOut(timeoutMs);
            socket.setSendTimeOut(1000);
            
            String msg = String.format("CANCEL:%s:%s:%d", order.getId(), order.getProduct(), order.getQuantity());
            
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

    // Clean shutdown
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

    // Helper class for reserve results
    private static class ReserveResult {
        final boolean success;
        final String reason;

        ReserveResult(boolean success, String reason) {
            this.success = success;
            this.reason = reason;
        }
    }
}