// Enhanced SellerStub.java - SAGA-compatible seller with reservation support
// Updated to work with Yana's 2-phase commit implementation

package seller;

import messaging.MessageUtils;
import org.zeromq.ZMQ;
import java.util.Random;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SellerStub {
    private static final Map<String, Integer> inventory = new ConcurrentHashMap<>();
    private static final Map<String, ReservationInfo> reservations = new ConcurrentHashMap<>();
    private static final Random rand = new Random();
    private static String sellerEndpoint;
    
    // Initialize inventory with random products
    static {
        inventory.put("laptop", 5 + rand.nextInt(10));
        inventory.put("phone", 8 + rand.nextInt(15));
        inventory.put("tablet", 3 + rand.nextInt(8));
        inventory.put("headphones", 6 + rand.nextInt(12));
        inventory.put("keyboard", 4 + rand.nextInt(10));
        inventory.put("mouse", 7 + rand.nextInt(15));
        inventory.put("monitor", 2 + rand.nextInt(6));
    }
    
    public static void start(String endpoint) {
        sellerEndpoint = endpoint;
        ZMQ.Socket socket = MessageUtils.createSocket("REP", true, endpoint);
        
        System.out.println("🏪 Seller online at " + endpoint);
        System.out.println("📦 Initial inventory:");
        printInventory();
        
        // Main message processing loop
        while (!Thread.currentThread().isInterrupted()) {
            try {
                long requestStart = System.currentTimeMillis();
                String message = socket.recvStr();
                
                if (message == null) continue;
                
                System.out.println("\n📨 Received: " + message + " at " + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")));
                
                String response = processMessage(message);
                socket.send(response);
                
                long responseTime = System.currentTimeMillis() - requestStart;
                System.out.println("📤 Response: " + response + " (took " + responseTime + "ms)");
                
            } catch (Exception e) {
                System.out.println("❌ Error processing message: " + e.getMessage());
                try {
                    socket.send("ERROR");
                } catch (Exception sendError) {
                    System.out.println("❌ Failed to send error response");
                }
            }
        }
    }
    
    private static String processMessage(String message) {
        try {
            // Handle different message types for SAGA pattern
            if (message.equals("PING") || message.equals("HEALTH_CHECK")) {
                return "HEALTHY";
            }
            
            if (message.startsWith("RESERVE:")) {
                return handleReservation(message);
            }
            
            if (message.startsWith("COMMIT:")) {
                return handleCommit(message);
            }
            
            if (message.startsWith("ROLLBACK:")) {
                return handleRollback(message);
            }
            
            // Legacy order handling (for backward compatibility)
            if (message.startsWith("ORDER:")) {
                return handleLegacyOrder(message);
            }
            
            if (message.startsWith("CANCEL:")) {
                return handleLegacyCancel(message);
            }
            
            return "UNKNOWN_COMMAND";
            
        } catch (Exception e) {
            System.out.println("❌ Error in processMessage: " + e.getMessage());
            return "ERROR";
        }
    }
    
    // SAGA Phase 1: Handle Reservation Request
    private static String handleReservation(String message) {
        // Format: RESERVE:reservationId:product:quantity
        String[] parts = message.split(":", 4);
        if (parts.length < 3) {
            return "INVALID_FORMAT";
        }
        
        String reservationId = parts[1];
        String productRequest = parts[2];
        
        // Parse product and quantity
        String[] productParts = productRequest.split(":");
        String product = productParts[0];
        int quantity = productParts.length > 1 ? Integer.parseInt(productParts[1]) : 1;
        
        System.out.println("🔒 Reservation request: " + quantity + "x " + product + " (ID: " + reservationId + ")");
        
        // Check if product exists and we have enough inventory
        if (!inventory.containsKey(product)) {
            System.out.println("   ❌ Product not available: " + product);
            return "PRODUCT_NOT_FOUND";
        }
        
        int availableQuantity = inventory.get(product);
        if (availableQuantity < quantity) {
            System.out.println("   ❌ Insufficient inventory: need " + quantity + ", have " + availableQuantity);
            return "INSUFFICIENT_INVENTORY";
        }
        
        // Simulate random failures (configurable failure rate)
        if (shouldSimulateFailure()) {
            System.out.println("   ⚠️  Simulated failure (random)");
            return "SELLER_ERROR";
        }
        
        // Reserve the products
        inventory.put(product, availableQuantity - quantity);
        reservations.put(reservationId, new ReservationInfo(product, quantity, LocalDateTime.now()));
        
        System.out.println("   ✅ Reserved: " + quantity + "x " + product);
        System.out.println("   📊 Remaining inventory: " + inventory.get(product));
        
        return "RESERVED";
    }
    
    // SAGA Phase 2: Handle Commit Request
    private static String handleCommit(String message) {
        // Format: COMMIT:reservationId
        String[] parts = message.split(":");
        if (parts.length != 2) {
            return "INVALID_FORMAT";
        }
        
        String reservationId = parts[1];
        ReservationInfo reservation = reservations.get(reservationId);
        
        if (reservation == null) {
            System.out.println("❌ Commit failed: Unknown reservation " + reservationId);
            return "RESERVATION_NOT_FOUND";
        }
        
        // Simulate random commit failures (rare)
        if (rand.nextDouble() < 0.05) { // 5% chance
            System.out.println("⚠️  Simulated commit failure for " + reservationId);
            return "COMMIT_FAILED";
        }
        
        // Remove reservation (it's now committed)
        reservations.remove(reservationId);
        
        System.out.println("✅ Committed: " + reservation.quantity + "x " + reservation.product + 
            " (Reservation: " + reservationId + ")");
        
        return "COMMITTED";
    }
    
    // SAGA Rollback: Handle Rollback Request
    private static String handleRollback(String message) {
        // Format: ROLLBACK:reservationId
        String[] parts = message.split(":");
        if (parts.length != 2) {
            return "INVALID_FORMAT";
        }
        
        String reservationId = parts[1];
        ReservationInfo reservation = reservations.get(reservationId);
        
        if (reservation == null) {
            System.out.println("⚠️  Rollback: Unknown reservation " + reservationId + " (already processed?)");
            return "ROLLED_BACK"; // Still return success for idempotency
        }
        
        // Return products to inventory
        String product = reservation.product;
        int quantity = reservation.quantity;
        int currentInventory = inventory.get(product);
        inventory.put(product, currentInventory + quantity);
        
        // Remove reservation
        reservations.remove(reservationId);
        
        System.out.println("🔄 Rolled back: " + quantity + "x " + product + 
            " (Reservation: " + reservationId + ")");
        System.out.println("   📊 Restored inventory: " + inventory.get(product));
        
        return "ROLLED_BACK";
    }
    
    // Legacy Order Handling (for backward compatibility)
    private static String handleLegacyOrder(String message) {
        String product = message.substring(6); // Remove "ORDER:"
        
        System.out.println("📦 Legacy order: " + product);
        
        if (!inventory.containsKey(product)) {
            System.out.println("   ❌ Product not found: " + product);
            return "REJECTED";
        }
        
        int available = inventory.get(product);
        if (available > 0 && !shouldSimulateFailure()) {
            inventory.put(product, available - 1);
            System.out.println("   ✅ Confirmed: " + product + " (remaining: " + inventory.get(product) + ")");
            return "CONFIRMED";
        } else {
            System.out.println("   ❌ Rejected: " + product + " (available: " + available + ")");
            return "REJECTED";
        }
    }
    
    // Legacy Cancel Handling
    private static String handleLegacyCancel(String message) {
        String product = message.substring(7); // Remove "CANCEL:"
        
        System.out.println("🔄 Legacy cancel: " + product);
        
        if (inventory.containsKey(product)) {
            inventory.put(product, inventory.get(product) + 1);
            System.out.println("   ✅ Cancelled: " + product + " (restored: " + inventory.get(product) + ")");
        }
        
        return "CANCELLED";
    }
    
    // Simulate random failures for testing
    private static boolean shouldSimulateFailure() {
        // 15% chance of failure to test SAGA rollback mechanisms
        return rand.nextDouble() < 0.15;
    }
    
    // Print current inventory status
    private static void printInventory() {
        System.out.println("📊 CURRENT INVENTORY:");
        for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
            System.out.println("   • " + entry.getKey() + ": " + entry.getValue() + " units");
        }
        
        if (!reservations.isEmpty()) {
            System.out.println("🔒 ACTIVE RESERVATIONS:");
            for (Map.Entry<String, ReservationInfo> entry : reservations.entrySet()) {
                ReservationInfo res = entry.getValue();
                System.out.println("   • " + entry.getKey() + ": " + res.quantity + "x " + res.product + 
                    " (since " + res.reservationTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + ")");
            }
        }
    }
    
    // Reservation Information Class
    private static class ReservationInfo {
        final String product;
        final int quantity;
        final LocalDateTime reservationTime;
        
        ReservationInfo(String product, int quantity, LocalDateTime reservationTime) {
            this.product = product;
            this.quantity = quantity;
            this.reservationTime = reservationTime;
        }
        
        @Override
        public String toString() {
            return quantity + "x " + product + " (reserved at " + 
                reservationTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + ")";
        }
    }
    
    // Cleanup expired reservations (could be called periodically)
    private static void cleanupExpiredReservations() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5); // 5-minute timeout
        
        reservations.entrySet().removeIf(entry -> {
            ReservationInfo reservation = entry.getValue();
            if (reservation.reservationTime.isBefore(cutoff)) {
                // Return products to inventory
                String product = reservation.product;
                int quantity = reservation.quantity;
                inventory.put(product, inventory.get(product) + quantity);
                
                System.out.println("🕐 Expired reservation cleaned up: " + entry.getKey() + 
                    " (" + reservation + ")");
                return true;
            }
            return false;
        });
    }
}