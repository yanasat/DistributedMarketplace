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
    private static SellerConfig config;

    // Start the seller at the given endpoint with configuration
    public static void start(String endpoint, SellerConfig sellerConfig) {
        config = sellerConfig != null ? sellerConfig : createDefaultConfig();
        
        ZMQ.Socket socket = MessageUtils.createSocket("REP", true, endpoint);
        System.out.println("Seller online at " + endpoint + " with config: " + config.toString());
        Random rand = new Random();

        // Main loop: handle incoming messages
        while (running && !Thread.currentThread().isInterrupted()) {
            long requestStart = System.currentTimeMillis();
            String msg = socket.recvStr();
            System.out.println("Received: " + msg);

            // Simulate network latency using normal distribution
            simulateLatency(rand);
            
            // Simulate crash (receive but don't process)
            if (simulateCrash(rand)) {
                System.out.println("[CRASH] Simulating crash: ignoring message");
                continue; // Don't send response
            }

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
                        
                        // Use configurable success probability
                        boolean canReserve = rand.nextDouble() < config.successProbability;
                        
                        if (canReserve) {
                            reservations.put(orderId, quantity);
                            response = "CONFIRMED:" + orderId;
                            System.out.println("[RESERVE] Confirmed order " + orderId + " for " + quantity + "x " + product);
                        } else {
                            response = "REJECTED:" + orderId;
                            System.out.println("[RESERVE] Rejected order " + orderId + " for " + quantity + "x " + product + " (no stock)");
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
                    boolean hasProduct = rand.nextDouble() < config.successProbability;
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

            // Simulate lost acknowledgment (process but don't respond)
            if (simulateLostAck(rand)) {
                System.out.println("[LOST_ACK] Simulating lost acknowledgment: not replying");
                continue; // Don't send response
            }
                
            socket.send(response);
            
            // Log seller response with timing
            long responseTime = System.currentTimeMillis() - requestStart;
            System.out.println("[MONITOR] Response: " + response + " in " + responseTime + "ms");
        }
        socket.close();
    }

    // Simulate network latency using normal distribution
    private static void simulateLatency(Random rand) {
        if (config.avgLatencyMs > 0) {
            try {
                // Normal distribution with mean=avgLatencyMs, stddev=avgLatencyMs/3
                double latency = Math.max(0, rand.nextGaussian() * (config.avgLatencyMs / 3.0) + config.avgLatencyMs);
                Thread.sleep((long) latency);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // Simulate seller crash (receive message but don't process)
    private static boolean simulateCrash(Random rand) {
        return rand.nextDouble() < config.crashProbability;
    }

    // Simulate lost acknowledgment (process but don't respond)
    private static boolean simulateLostAck(Random rand) {
        return rand.nextDouble() < config.lostAckProbability;
    }

    // Create default configuration for fallback
    private static SellerConfig createDefaultConfig() {
        SellerConfig defaultConfig = new SellerConfig();
        defaultConfig.port = 5555;
        defaultConfig.products = Map.of("default", 100);
        defaultConfig.crashProbability = 0.05;
        defaultConfig.lostAckProbability = 0.02;
        defaultConfig.avgLatencyMs = 100;
        defaultConfig.successProbability = 0.8;
        return defaultConfig;
    }

    // Overloaded method for backward compatibility
    public static void start(String endpoint) {
        start(endpoint, null);
    }

    public static void stop() {
        running = false;
    }
}