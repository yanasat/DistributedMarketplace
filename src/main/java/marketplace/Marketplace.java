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

    public Marketplace(List<String> sellerEndpoints) {
        this(sellerEndpoints, 2000); // Default 2 second timeout
    }

    public Marketplace(List<String> sellerEndpoints, int timeoutMs) {
        this.sellerEndpoints = sellerEndpoints;
        this.timeoutMs = timeoutMs;
    }

    public void placeOrder(String product, int quantity) {
        Order order = new Order(product, quantity);
        System.out.println("=== Starting SAGA transaction for order: " + order.getId() + " ===");

        // Phase 1: RESERVE - Send reservation requests to all sellers
        List<Future<Boolean>> futures = sellerEndpoints.stream()
                .map(endpoint -> executor.submit(() -> reserve(endpoint, order)))
                .collect(Collectors.toList());

        // Collect responses with timeout
        for (int i = 0; i < futures.size(); i++) {
            try {
                boolean confirmed = futures.get(i).get(timeoutMs, TimeUnit.MILLISECONDS);
                String endpoint = sellerEndpoints.get(i);
                if (confirmed) {
                    order.setStatus(endpoint, Status.CONFIRMED);
                    System.out.println("✅ Seller " + endpoint + " CONFIRMED reservation");
                } else {
                    order.setStatus(endpoint, Status.REJECTED);
                    System.out.println("❌ Seller " + endpoint + " REJECTED reservation");
                }
            } catch (Exception e) {
                String endpoint = sellerEndpoints.get(i);
                System.out.println("⏰ Timeout/Error for seller " + endpoint + ": " + e.getMessage());
                order.setStatus(endpoint, Status.REJECTED);
            }
        }

        // Phase 2: Decision - COMMIT or ROLLBACK
        if (order.isFullyConfirmed()) {
            System.out.println("🎉 Order CONFIRMED by all sellers. Sending COMMIT...");
            commitOrder(order);
        } else {
            System.out.println("🔄 One or more sellers REJECTED. Rolling back...");
            rollbackOrder(order);
        }
        
        System.out.println("=== SAGA transaction completed for order: " + order.getId() + " ===\n");
    }

    private boolean reserve(String endpoint, Order order) {
        try (ZMQ.Socket socket = MessageUtils.createSocket("REQ", false, endpoint)) {
            socket.setReceiveTimeOut(timeoutMs);
            socket.setSendTimeOut(1000);
            
            String msg = String.format("RESERVE:%s:%s:%d", order.getId(), order.getProduct(), order.getQuantity());
            socket.send(msg);
            
            String reply = socket.recvStr();
            if (reply != null) {
                System.out.println("RESERVE response from " + endpoint + ": " + reply);
                return reply.startsWith("CONFIRMED");
            } else {
                System.out.println("No response from " + endpoint + " (timeout)");
                return false;
            }
        } catch (Exception e) {
            System.out.println("Error communicating with " + endpoint + ": " + e.getMessage());
            return false;
        }
    }

    private void commitOrder(Order order) {
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
            socket.send(msg);
            
            String reply = socket.recvStr();
            if (reply != null) {
                System.out.println("COMMIT response from " + endpoint + ": " + reply);
            }
        } catch (Exception e) {
            System.out.println("Error during COMMIT to " + endpoint + ": " + e.getMessage());
        }
    }

    private void rollbackOrder(Order order) {
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
            socket.send(msg);
            
            String reply = socket.recvStr();
            if (reply != null) {
                System.out.println("ROLLBACK response from " + endpoint + ": " + reply);
            }
        } catch (Exception e) {
            System.out.println("Error during ROLLBACK to " + endpoint + ": " + e.getMessage());
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
}