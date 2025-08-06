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
        
        // Initialisiere Inventar aus Config
        initializeInventory();
        
        ZMQ.Socket socket = MessageUtils.createSocket("REP", true, endpoint);
        System.out.println("Seller online at " + endpoint);
        System.out.println("Initial inventory: " + inventory);
        System.out.println("Config: " + config.toString());
        
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
                    // Legacy support
                    String product = msg.substring(6);
                    boolean hasProduct = hasStock(product, 1);
                    response = hasProduct ? "CONFIRMED" : "REJECTED";
                }
                
            } catch (Exception e) {
                System.out.println("[ERROR] Failed to process message: " + msg + " - " + e.getMessage());
                response = "ERROR:" + e.getMessage();
            }

            if (simulateLostAck(rand)) {
                System.out.println("[LOST_ACK] Simulating lost acknowledgment: not replying");
                continue;
            }
                
            socket.send(response);
            
            long responseTime = System.currentTimeMillis() - requestStart;
            System.out.println("[MONITOR] Response: " + response + " in " + responseTime + "ms");
            
            // Zeige aktuelles Inventar nach jeder Operation
            if (!msg.equals("HEALTH_CHECK")) {
                printInventoryStatus();
            }
        }
        socket.close();
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