package seller;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.zeromq.ZMQ;

import messaging.MessageUtils;


public class SellerStub {
    private static volatile boolean running = true;
    private static final Map<String, Integer> reservations = new HashMap<>();
    private static SellerConfig config;


    public static void start(String endpoint, SellerConfig sellerConfig) {
        config = sellerConfig != null ? sellerConfig : createDefaultConfig();
        
        ZMQ.Socket socket = MessageUtils.createSocket("REP", true, endpoint);
        System.out.println("Seller online at " + endpoint + " with config: " + config.toString());
        Random rand = new Random();


        while (running && !Thread.currentThread().isInterrupted()) {
            long requestStart = System.currentTimeMillis();
            String msg = socket.recvStr();
            System.out.println("Received: " + msg);

            
            simulateLatency(rand);
            
            
            if (simulateCrash(rand)) {
                System.out.println("[CRASH] Simulating crash: ignoring message");
                continue; 
            }

            String response = "UNKNOWN";
            
            try {
                
                if (msg.startsWith("RESERVE:")) {

                    String[] parts = msg.split(":");
                    if (parts.length >= 4) {
                        String orderId = parts[1];
                        String product = parts[2];
                        int quantity = Integer.parseInt(parts[3]);
                        
                        
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
                    
                    response = "HEALTHY";
                    System.out.println("[HEALTH] Health check responded");
                }
                
                else if (msg.startsWith("ORDER:")) {
                    String product = msg.substring(6); 
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

            
            if (simulateLostAck(rand)) {
                System.out.println("[LOST_ACK] Simulating lost acknowledgment: not replying");
                continue; 
            }
                
            socket.send(response);
            
            
            long responseTime = System.currentTimeMillis() - requestStart;
            System.out.println("[MONITOR] Response: " + response + " in " + responseTime + "ms");
        }
        socket.close();
    }

    
    private static void simulateLatency(Random rand) {
        if (config.avgLatencyMs > 0) {
            try {
                
                double latency = Math.max(0, rand.nextGaussian() * (config.avgLatencyMs / 3.0) + config.avgLatencyMs);
                Thread.sleep((long) latency);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    
    private static boolean simulateCrash(Random rand) {
        return rand.nextDouble() < config.crashProbability;
    }

    
    private static boolean simulateLostAck(Random rand) {
        return rand.nextDouble() < config.lostAckProbability;
    }

    
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

    
    public static void start(String endpoint) {
        start(endpoint, null);
    }

    public static void stop() {
        running = false;
    }
}