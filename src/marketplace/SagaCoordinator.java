package marketplace;

import messaging.MessageUtils;
import model.MultiProductOrder;
import model.Product;
import org.zeromq.ZMQ;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * SagaCoordinator implements the 2-phase commit SAGA pattern
 * Phase 1: Reserve products from all relevant sellers
 * Phase 2: Commit all reservations OR rollback all if any failed
 */
public class SagaCoordinator {
    private final List<String> sellerEndpoints;
    private final ExecutorService executorService;
    private final Map<String, ReservationStatus> activeReservations = new ConcurrentHashMap<>();

    private enum ReservationStatus {
        RESERVED, COMMITTED, ROLLED_BACK, FAILED
    }

    public SagaCoordinator(List<String> sellerEndpoints) {
        this.sellerEndpoints = sellerEndpoints;
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Execute a multi-product order using 2-phase commit SAGA
     */
    public boolean executeOrder(MultiProductOrder order) {
        String orderId = order.getOrderId();
        System.out.println("Starting SAGA for order: " + orderId);

        try {
            // PHASE 1: Reserve products from all sellers
            if (!reservePhase(order)) {
                System.out.println("Reserve phase failed for order: " + orderId);
                return false;
            }

            // PHASE 2: Commit all reservations
            if (!commitPhase(order)) {
                System.out.println("Commit phase failed for order: " + orderId);
                // Rollback if commit fails
                rollbackPhase(order);
                return false;
            }

            System.out.println("SAGA completed successfully for order: " + orderId);
            return true;

        } catch (Exception e) {
            System.err.println("SAGA failed for order " + orderId + ": " + e.getMessage());
            rollbackPhase(order);
            return false;
        }
    }

    /**
     * Phase 1: Reserve products from all relevant sellers
     */
    private boolean reservePhase(MultiProductOrder order) {
        System.out.println("Phase 1: Reserving products for order " + order.getOrderId());

        Map<String, CompletableFuture<Boolean>> reservationFutures = new HashMap<>();

        // Group products by seller
        Map<String, List<Product>> productsBySeller = groupProductsBySeller(order);

        // Send reserve requests to all sellers concurrently
        for (Map.Entry<String, List<Product>> entry : productsBySeller.entrySet()) {
            String sellerEndpoint = entry.getKey();
            List<Product> products = entry.getValue();

            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                return reserveFromSeller(sellerEndpoint, products, order.getOrderId());
            }, executorService);

            reservationFutures.put(sellerEndpoint, future);
        }

        // Wait for all reservations to complete
        boolean allReserved = true;
        for (Map.Entry<String, CompletableFuture<Boolean>> entry : reservationFutures.entrySet()) {
            try {
                boolean reserved = entry.getValue().get(30, TimeUnit.SECONDS); // 30 second timeout
                if (!reserved) {
                    allReserved = false;
                    System.out.println("Reservation failed for seller: " + entry.getKey());
                }
            } catch (Exception e) {
                allReserved = false;
                System.err.println("Reservation timeout/error for seller " + entry.getKey() + ": " + e.getMessage());
            }
        }

        return allReserved;
    }

    /**
     * Phase 2: Commit all reservations
     */
    private boolean commitPhase(MultiProductOrder order) {
        System.out.println("Phase 2: Committing reservations for order " + order.getOrderId());

        Map<String, CompletableFuture<Boolean>> commitFutures = new HashMap<>();

        // Group products by seller
        Map<String, List<Product>> productsBySeller = groupProductsBySeller(order);

        // Send commit requests to all sellers concurrently
        for (String sellerEndpoint : productsBySeller.keySet()) {
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                return commitWithSeller(sellerEndpoint, order.getOrderId());
            }, executorService);

            commitFutures.put(sellerEndpoint, future);
        }

        // Wait for all commits to complete
        boolean allCommitted = true;
        for (Map.Entry<String, CompletableFuture<Boolean>> entry : commitFutures.entrySet()) {
            try {
                boolean committed = entry.getValue().get(30, TimeUnit.SECONDS);
                if (!committed) {
                    allCommitted = false;
                    System.out.println("Commit failed for seller: " + entry.getKey());
                }
            } catch (Exception e) {
                allCommitted = false;
                System.err.println("Commit timeout/error for seller " + entry.getKey() + ": " + e.getMessage());
            }
        }

        return allCommitted;
    }

    /**
     * Rollback phase: Cancel all reservations
     */
    private void rollbackPhase(MultiProductOrder order) {
        System.out.println("Rollback phase: Cancelling reservations for order " + order.getOrderId());

        Map<String, List<Product>> productsBySeller = groupProductsBySeller(order);

        // Send rollback requests to all sellers concurrently
        for (String sellerEndpoint : productsBySeller.keySet()) {
            executorService.submit(() -> {
                rollbackWithSeller(sellerEndpoint, order.getOrderId());
            });
        }
    }

    private boolean reserveFromSeller(String sellerEndpoint, List<Product> products, String orderId) {
        ZMQ.Socket socket = null;
        try {
            socket = MessageUtils.createSocket("REQ", false, sellerEndpoint);
            socket.setSendTimeOut(5000); // 5 second timeout
            socket.setReceiveTimeOut(5000);

            // Build reserve message
            StringBuilder message = new StringBuilder("RESERVE:" + orderId);
            for (Product product : products) {
                message.append(";").append(product.getName()).append(":").append(product.getQuantity());
            }

            socket.send(message.toString());
            String response = socket.recvStr();

            boolean success = "RESERVED".equals(response);
            if (success) {
                activeReservations.put(sellerEndpoint + ":" + orderId, ReservationStatus.RESERVED);
            }

            return success;

        } catch (Exception e) {
            System.err.println("Error reserving from seller " + sellerEndpoint + ": " + e.getMessage());
            return false;
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

    private boolean commitWithSeller(String sellerEndpoint, String orderId) {
        ZMQ.Socket socket = null;
        try {
            socket = MessageUtils.createSocket("REQ", false, sellerEndpoint);
            socket.setSendTimeOut(5000);
            socket.setReceiveTimeOut(5000);

            socket.send("COMMIT:" + orderId);
            String response = socket.recvStr();

            boolean success = "COMMITTED".equals(response);
            if (success) {
                activeReservations.put(sellerEndpoint + ":" + orderId, ReservationStatus.COMMITTED);
            }

            return success;

        } catch (Exception e) {
            System.err.println("Error committing with seller " + sellerEndpoint + ": " + e.getMessage());
            return false;
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

    private boolean rollbackWithSeller(String sellerEndpoint, String orderId) {
        ZMQ.Socket socket = null;
        try {
            socket = MessageUtils.createSocket("REQ", false, sellerEndpoint);
            socket.setSendTimeOut(5000);
            socket.setReceiveTimeOut(5000);

            socket.send("ROLLBACK:" + orderId);
            String response = socket.recvStr();

            boolean success = "ROLLED_BACK".equals(response);
            if (success) {
                activeReservations.put(sellerEndpoint + ":" + orderId, ReservationStatus.ROLLED_BACK);
            }

            return success;

        } catch (Exception e) {
            System.err.println("Error rolling back with seller " + sellerEndpoint + ": " + e.getMessage());
            return false;
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

    private Map<String, List<Product>> groupProductsBySeller(MultiProductOrder order) {
        Map<String, List<Product>> productsBySeller = new HashMap<>();

        for (Product product : order.getProducts()) {
            // For now, use discovery mechanism to find seller for each product
            String sellerEndpoint = discoverSellerForProduct(product);

            productsBySeller.computeIfAbsent(sellerEndpoint, k -> new ArrayList<>()).add(product);
        }

        return productsBySeller;
    }

    private String discoverSellerForProduct(Product product) {
        // Simple round-robin discovery - in production, implement proper service discovery
        // For now, just cycle through available sellers
        int hash = product.getName().hashCode();
        int index = Math.abs(hash % sellerEndpoints.size());
        return sellerEndpoints.get(index);
    }

    public void shutdown() {
        executorService.shutdown();
    }
}
