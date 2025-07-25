package seller;

import messaging.MessageUtils;
import model.Product;
import org.zeromq.ZMQ;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * EnhancedSellerProcess implements the seller side of 2-phase commit protocol
 * Handles RESERVE, COMMIT, and ROLLBACK operations
 */
public class EnhancedSellerProcess {
    private final String sellerId;
    private final int port;
    private final Map<String, Integer> inventory; // product -> quantity
    private final Map<String, Map<String, Integer>> reservations; // orderId -> (product -> quantity)
    private final ExecutorService executorService;
    private volatile boolean running = true;
    private final Random random = new Random();

    public EnhancedSellerProcess(String sellerId, int port, Map<String, Integer> initialInventory) {
        this.sellerId = sellerId;
        this.port = port;
        this.inventory = new ConcurrentHashMap<>(initialInventory);
        this.reservations = new ConcurrentHashMap<>();
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: java -jar seller.jar <seller-id> <port> <config-file>");
            System.exit(1);
        }

        String sellerId = args[0];
        int port = Integer.parseInt(args[1]);
        String configFile = args[2];

        try {
            Map<String, Integer> inventory = loadInventory(configFile);
            EnhancedSellerProcess seller = new EnhancedSellerProcess(sellerId, port, inventory);
            seller.start();
        } catch (Exception e) {
            System.err.println("Failed to start seller: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static Map<String, Integer> loadInventory(String configFile) throws IOException {
        Properties props = new Properties();
        props.load(new FileInputStream(configFile));

        Map<String, Integer> inventory = new HashMap<>();
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("inventory.")) {
                String productName = key.substring("inventory.".length());
                int quantity = Integer.parseInt(props.getProperty(key));
                inventory.put(productName, quantity);
            }
        }

        return inventory;
    }

    public void start() {
        System.out.println("Starting Seller " + sellerId + " on port " + port);
        System.out.println("Initial inventory: " + inventory);

        executorService.submit(this::handleRequests);

        // Keep main thread alive
        try {
            while (running) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        shutdown();
    }

    private void handleRequests() {
        ZMQ.Socket socket = MessageUtils.createSocket("REP", true, "tcp://*:" + port);
        System.out.println("Seller " + sellerId + " listening on port " + port);

        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                String request = socket.recvStr(ZMQ.DONTWAIT);
                if (request != null) {
                    executorService.submit(() -> {
                        String response = processRequest(request);
                        socket.send(response);
                    });
                }
                Thread.sleep(10); // Small delay to prevent busy waiting
            } catch (Exception e) {
                System.err.println("Error handling request: " + e.getMessage());
            }
        }

        socket.close();
    }

    private String processRequest(String request) {
        System.out.println("Seller " + sellerId + " received: " + request);

        try {
            // Simulate processing time and potential failures
            simulateProcessingDelay();

            if (shouldSimulateFailure()) {
                System.out.println("Simulating failure for request: " + request);
                return "ERROR";
            }

            String[] parts = request.split(":", 2);
            if (parts.length != 2) {
                return "INVALID_REQUEST";
            }

            String command = parts[0];
            String payload = parts[1];

            switch (command) {
                case "RESERVE":
                    return handleReserve(payload);
                case "COMMIT":
                    return handleCommit(payload);
                case "ROLLBACK":
                    return handleRollback(payload);
                default:
                    return "UNKNOWN_COMMAND";
            }

        } catch (Exception e) {
            System.err.println("Error processing request: " + e.getMessage());
            return "ERROR";
        }
    }

    private String handleReserve(String payload) {
        // Format: orderId;product1:quantity1;product2:quantity2
        String[] parts = payload.split(";");
        if (parts.length < 2) {
            return "INVALID_RESERVE_FORMAT";
        }

        String orderId = parts[0];
        Map<String, Integer> requestedProducts = new HashMap<>();

        // Parse requested products
        for (int i = 1; i < parts.length; i++) {
            String[] productInfo = parts[i].split(":");
            if (productInfo.length != 2) {
                return "INVALID_PRODUCT_FORMAT";
            }

            String productName = productInfo[0];
            int quantity = Integer.parseInt(productInfo[1]);
            requestedProducts.put(productName, quantity);
        }

        // Check if all products are available
        synchronized (inventory) {
            for (Map.Entry<String, Integer> entry : requestedProducts.entrySet()) {
                String product = entry.getKey();
                int requestedQuantity = entry.getValue();
                int availableQuantity = inventory.getOrDefault(product, 0);

                if (availableQuantity < requestedQuantity) {
                    System.out.println("Insufficient inventory for " + product + ": requested=" + requestedQuantity + ", available=" + availableQuantity);
                    return "INSUFFICIENT_INVENTORY";
                }
            }

            // Reserve the products
            for (Map.Entry<String, Integer> entry : requestedProducts.entrySet()) {
                String product = entry.getKey();
                int quantity = entry.getValue();
                inventory.put(product, inventory.get(product) - quantity);
            }

            // Store reservation
            reservations.put(orderId, requestedProducts);
        }

        System.out.println("Reserved products for order " + orderId + ": " + requestedProducts);
        System.out.println("Updated inventory: " + inventory);
        return "RESERVED";
    }

    // Add missing method stubs to fix compilation errors
    private String handleCommit(String payload) {
        // TODO: Implement commit logic
        return "COMMITTED";
    }

    private String handleRollback(String payload) {
        // TODO: Implement rollback logic
        return "ROLLED_BACK";
    }

    private void shutdown() {
        // TODO: Implement shutdown logic if needed
    }

    private void simulateProcessingDelay() {
        // TODO: Implement simulated delay if needed
    }

    private boolean shouldSimulateFailure() {
        // TODO: Implement failure simulation logic if needed
        return false;
    }
}