package seller;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.zeromq.ZMQ;
import messaging.MessageUtils;

public class SellerStub {
    private static volatile boolean running = true;
    private static final Map<String, Integer> reservations = new HashMap<>();
    
    // NEU: Echtes Inventar-Management
    private static final Map<String, Integer> inventory = new ConcurrentHashMap<>();
    private static final Map<String, Integer> reservedStock = new ConcurrentHashMap<>();
    
    private static SellerConfig config;

    public static void start(String endpoint, SellerConfig sellerConfig) {
     config = sellerConfig != null ? sellerConfig : createDefaultConfig();
    initializeInventory();
    
    ZMQ.Socket socket = null;
    try {
        socket = MessageUtils.createSocket("REP", true, endpoint);
        System.out.println("Seller online at " + endpoint);
        System.out.println("Initial inventory: " + inventory);
        System.out.println("Config: " + config.toString());
        
        Random rand = new Random();

        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                long requestStart = System.currentTimeMillis();
                
                // ROBUST message receiving with multiple fallbacks
                String msg = null;
                try {
                    byte[] msgBytes = socket.recv(0); // Non-blocking
                    if (msgBytes != null && msgBytes.length > 0) {
                        // Try UTF-8 first
                        msg = new String(msgBytes, java.nio.charset.StandardCharsets.UTF_8).trim();
                        
                        // Fallback: ASCII if UTF-8 fails
                        if (msg.isEmpty() || msg.contains("�")) {
                            msg = new String(msgBytes, java.nio.charset.StandardCharsets.US_ASCII).trim();
                        }
                        
                        // Last resort: filter printable chars only
                        if (msg.contains("�") || msg.length() == 0) {
                            msg = new String(msgBytes).replaceAll("[^\\p{Print}]", "").trim();
                        }
                    }
                } catch (Exception recvError) {
                    System.out.println("[RECV_ERROR] " + recvError.getMessage());
                    continue;
                }
                
                if (msg == null || msg.isEmpty()) {
                    System.out.println("[EMPTY_MSG] Received empty message, skipping");
                    continue;
                }
                
                System.out.println("Received: " + msg);

                // Simulate network issues
                simulateLatency(rand);
                
                if (simulateCrash(rand)) {
                    System.out.println("[CRASH] Simulating crash: ignoring message");
                    continue;
                }

                String response = "UNKNOWN";
                
                try {
                    // Parse message with robust error handling
                    if (msg.startsWith("RESERVE:")) {
                        response = handleReserve(msg);
                    }
                    else if (msg.startsWith("COMMIT:")) {
                        response = handleCommit(msg);
                    }
                    else if (msg.startsWith("CANCEL:") || msg.startsWith("ROLLBACK:")) {
                        response = handleRollback(msg);
                    }
                    else if (msg.equals("HEALTH_CHECK")) {
                        response = "HEALTHY";
                        System.out.println("[HEALTH] Health check responded");
                    }
                    else if (msg.startsWith("ORDER:")) {
                        String product = msg.length() > 6 ? msg.substring(6) : "unknown";
                        boolean hasProduct = hasStock(product, 1);
                        response = hasProduct ? "CONFIRMED" : "REJECTED";
                        System.out.println("[LEGACY] " + product + " → " + response);
                    }
                    else {
                        System.out.println("[UNKNOWN] Unknown message format: " + msg);
                        response = "ERROR:UNKNOWN_FORMAT";
                    }
                    
                } catch (Exception processError) {
                    System.out.println("[PROCESS_ERROR] Failed to process '" + msg + "': " + processError.getMessage());
                    response = "ERROR:" + processError.getMessage();
                }

                // Simulate lost acknowledgments
                if (simulateLostAck(rand)) {
                    System.out.println("[LOST_ACK] Simulating lost acknowledgment: not replying");
                    continue;
                }
                
                // ROBUST response sending
                try {
                    // Ensure response is valid ASCII
                    response = response.replaceAll("[^\\p{ASCII}]", "");
                    
                    byte[] responseBytes = response.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    socket.send(responseBytes, 0);
                    
                    long responseTime = System.currentTimeMillis() - requestStart;
                    System.out.println("[MONITOR] Response: " + response + " in " + responseTime + "ms");
                    
                } catch (Exception sendError) {
                    System.out.println("[SEND_ERROR] Failed to send response: " + sendError.getMessage());
                    continue;
                }
                
                // Show inventory (except for health checks)
                if (!msg.equals("HEALTH_CHECK")) {
                    printInventoryStatus();
                }
                
            } catch (Exception outerError) {
                System.out.println(" [FATAL] Outer loop error: " + outerError.getMessage());
                outerError.printStackTrace();
                
                // Try to recover
                try { Thread.sleep(100); } catch (InterruptedException ie) { break; }
            }
        }
        
    } catch (Exception startupError) {
        System.out.println(" [STARTUP_ERROR] " + startupError.getMessage());
        startupError.printStackTrace();
        
    } finally {
        if (socket != null) {
            try {
                socket.close();
                System.out.println("🔌 Socket closed for " + endpoint);
            } catch (Exception closeError) {
                System.out.println("⚠️ Error closing socket: " + closeError.getMessage());
            }
        }
    }
}

    private static synchronized String handleReserve(String msg) {
        String[] parts = msg.split(":");
        if (parts.length >= 4) {
            String orderId = parts[1];
            String product = parts[2];
            int quantity = Integer.parseInt(parts[3]);
            
            // Echte Bestandsprüfung
            if (canReserve(product, quantity)) {
                // Reserviere den Bestand
                int currentReserved = reservedStock.getOrDefault(product, 0);
                reservedStock.put(product, currentReserved + quantity);
                
                // Speichere Reservierung für späteren Commit/Rollback
                reservations.put(orderId, quantity);
                
                System.out.println("[RESERVE] ✅ Confirmed order " + orderId + 
                                 " for " + quantity + "x " + product);
                return "CONFIRMED:" + orderId;
            } else {
                System.out.println("[RESERVE] ❌ Rejected order " + orderId + 
                                 " for " + quantity + "x " + product + " (insufficient stock)");
                return "REJECTED:" + orderId;
            }
        }
        return "ERROR:INVALID_RESERVE_FORMAT";
    }

    private static synchronized String handleCommit(String msg) {
        String[] parts = msg.split(":");
        if (parts.length >= 4) {
            String orderId = parts[1];
            String product = parts[2];
            int quantity = Integer.parseInt(parts[3]);
            
            // Entferne aus Inventar (war bereits reserviert)
            int currentStock = inventory.getOrDefault(product, 0);
            inventory.put(product, Math.max(0, currentStock - quantity));
            
            // Entferne aus Reservierungen
            int currentReserved = reservedStock.getOrDefault(product, 0);
            reservedStock.put(product, Math.max(0, currentReserved - quantity));
            
            reservations.remove(orderId);
            
            System.out.println("[COMMIT] ✅ Committed order " + orderId + 
                             " for " + quantity + "x " + product);
            return "COMMITTED:" + orderId;
        }
        return "ERROR:INVALID_COMMIT_FORMAT";
    }

    private static synchronized String handleRollback(String msg) {
        String[] parts = msg.split(":");
        if (parts.length >= 4) {
            String orderId = parts[1];
            String product = parts[2];
            int quantity = Integer.parseInt(parts[3]);
            
            // Gebe reservierten Bestand frei
            int currentReserved = reservedStock.getOrDefault(product, 0);
            reservedStock.put(product, Math.max(0, currentReserved - quantity));
            
            reservations.remove(orderId);
            
            System.out.println("[ROLLBACK] ↩️ Cancelled order " + orderId + 
                             " for " + quantity + "x " + product);
            return "ROLLED_BACK:" + orderId;
        }
        return "ERROR:INVALID_ROLLBACK_FORMAT";
    }

    private static boolean canReserve(String product, int quantity) {
        int available = inventory.getOrDefault(product, 0);
        int reserved = reservedStock.getOrDefault(product, 0);
        int actuallyAvailable = available - reserved;
        
        return actuallyAvailable >= quantity;
    }

    private static boolean hasStock(String product, int quantity) {
        return canReserve(product, quantity);
    }

    private static void initializeInventory() {
        if (config.products != null) {
            inventory.putAll(config.products);
        } else {
            // Default inventory
            inventory.put("laptop", 50);
            inventory.put("smartphone", 30);
            inventory.put("tablet", 20);
        }
        
        // Initialize reserved stock tracking
        for (String product : inventory.keySet()) {
            reservedStock.put(product, 0);
        }
    }

    private static void printInventoryStatus() {
        System.out.println("📦 INVENTORY STATUS:");
        for (String product : inventory.keySet()) {
            int total = inventory.get(product);
            int reserved = reservedStock.getOrDefault(product, 0);
            int available = total - reserved;
            System.out.println("   " + product + ": " + available + " available (" + 
                             total + " total, " + reserved + " reserved)");
        }
    }

    // Simulation methods unchanged...
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
        defaultConfig.products = Map.of(
            "laptop", 50, 
            "smartphone", 30, 
            "tablet", 20,
            "camera", 15,
            "headphones", 100
        );
        defaultConfig.crashProbability = 0.05;
        defaultConfig.lostAckProbability = 0.02;
        defaultConfig.avgLatencyMs = 100;
        defaultConfig.successProbability = 1.0; // Nicht mehr relevant - echte Bestandsprüfung
        return defaultConfig;
    }

    public static void start(String endpoint) {
        start(endpoint, null);
    }

    public static void stop() {
        running = false;
    }
}