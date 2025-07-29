package seller;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.zeromq.ZMQ;

import messaging.MessageUtils;

// SellerStub simulates a seller that can confirm or reject orders randomly
public class SellerStub {
    private static volatile boolean running = true;
    private static final Map<String, Integer> reservations = new HashMap<>();

    // Start the seller at the given endpoint
    public static void start(String endpoint) {
        ZMQ.Socket socket = MessageUtils.createSocket("REP", true, endpoint);
        System.out.println("Seller online at " + endpoint);
        Random rand = new Random();

        // Main loop: handle incoming messages
        while (running && !Thread.currentThread().isInterrupted()) {
            long requestStart = System.currentTimeMillis();
            String msg = socket.recvStr();
            System.out.println("Received: " + msg);

            String response = "UNKNOWN";
            
            try {
                // Handle new SAGA protocol
                if (msg.startsWith("RESERVE:")) {
                    // Format: RESERVE:orderId:product:quantity
                    String[] parts = msg.split(":");
                    if (parts.length >= 4) {
                        String orderId = parts[1];
                        String product = parts[2];
                        int quantity = Integer.parseInt(parts[3]);
                        
                        // 70% chance to confirm reservation
                        boolean canReserve = rand.nextDouble() < 0.7;
                        
                        if (canReserve) {
                            reservations.put(orderId, quantity);
                            response = "CONFIRMED:" + orderId;
                            System.out.println("[RESERVE] Confirmed order " + orderId + " for " + quantity + "x " + product);
                        } else {
                            response = "REJECTED:" + orderId;
                            System.out.println("[RESERVE] Rejected order " + orderId + " for " + quantity + "x " + product);
                        }
                    }
                }
                else if (msg.startsWith("COMMIT:")) {
                    // Format: COMMIT:orderId:product:quantity
                    String[] parts = msg.split(":");
                    if (parts.length >= 4) {
                        String orderId = parts[1];
                        String product = parts[2];
                        int quantity = Integer.parseInt(parts[3]);
                        
                        reservations.remove(orderId);
                        response = "COMMITTED:" + orderId;
                        System.out.println("[COMMIT] Committed order " + orderId + " for " + quantity + "x " + product);
                    }
                }
                else if (msg.startsWith("CANCEL:") || msg.startsWith("ROLLBACK:")) {
                    // Format: CANCEL:orderId:product:quantity
                    String[] parts = msg.split(":");
                    if (parts.length >= 4) {
                        String orderId = parts[1];
                        String product = parts[2];
                        int quantity = Integer.parseInt(parts[3]);
                        
                        reservations.remove(orderId);
                        response = "ROLLED_BACK:" + orderId;
                        System.out.println("[ROLLBACK] Cancelled order " + orderId + " for " + quantity + "x " + product);
                    }
                }
                else if (msg.equals("HEALTH_CHECK")) {
                    // Respond to health check requests
                    response = "HEALTHY";
                    System.out.println("[HEALTH] Health check responded");
                }
                // Legacy protocol support
                else if (msg.startsWith("ORDER:")) {
                    String product = msg.substring(6); // Extract product name
                    boolean hasProduct = rand.nextBoolean(); // 50% chance to have the product
                    response = hasProduct ? "CONFIRMED" : "REJECTED";
                    
                    if (hasProduct) {
                        System.out.println("[LEGACY] Accepting order for: " + product);
                    } else {
                        System.out.println("[LEGACY] Declining order for: " + product);
                    }
                }
                
            } catch (Exception e) {
                System.out.println("[ERROR] Failed to process message: " + msg + " - " + e.getMessage());
                response = "ERROR";
            }
                
            socket.send(response);
            
            // Log seller response with timing
            long responseTime = System.currentTimeMillis() - requestStart;
            System.out.println("[MONITOR] Response: " + response + " in " + responseTime + "ms");
        }
        socket.close();
    }

    public static void stop() {
        running = false;
    }
}