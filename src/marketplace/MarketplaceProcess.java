package marketplace;

import messaging.MessageUtils;
import model.MultiProductOrder;
import model.Product;
import org.zeromq.ZMQ;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MarketplaceProcess - Main entry point for a marketplace instance
 * Each marketplace runs as a separate process with its own configuration
 */
public class MarketplaceProcess {
    private final String marketplaceId;
    private final int clientPort;
    private final List<String> sellerEndpoints;
    private final SagaCoordinator sagaCoordinator;
    private final ExecutorService executorService;
    private volatile boolean running = true;

    public MarketplaceProcess(String marketplaceId, int clientPort, List<String> sellerEndpoints) {
        this.marketplaceId = marketplaceId;
        this.clientPort = clientPort;
        this.sellerEndpoints = sellerEndpoints;
        this.sagaCoordinator = new SagaCoordinator(sellerEndpoints);
        this.executorService = Executors.newVirtualThreadPerTaskExecutor(); // Using virtual threads as suggested in notes
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: java -jar marketplace.jar <marketplace-id> <client-port> <config-file>");
            System.exit(1);
        }

        String marketplaceId = args[0];
        int clientPort = Integer.parseInt(args[1]);
        String configFile = args[2];

        try {
            List<String> sellerEndpoints = loadSellerEndpoints(configFile);
            MarketplaceProcess marketplace = new MarketplaceProcess(marketplaceId, clientPort, sellerEndpoints);
            marketplace.start();
        } catch (Exception e) {
            System.err.println("Failed to start marketplace: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static List<String> loadSellerEndpoints(String configFile) throws IOException {
        Properties props = new Properties();
        props.load(new FileInputStream(configFile));

        List<String> endpoints = new ArrayList<>();
        String sellersStr = props.getProperty("sellers.endpoints");
        if (sellersStr != null) {
            endpoints.addAll(Arrays.asList(sellersStr.split(",")));
        }

        return endpoints;
    }

    public void start() {
        System.out.println("Starting Marketplace " + marketplaceId + " on port " + clientPort);

        // Start client request handler
        executorService.submit(this::handleClientRequests);

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

    private void handleClientRequests() {
        ZMQ.Socket socket = MessageUtils.createSocket("REP", true, "tcp://*:" + clientPort);
        System.out.println("Marketplace " + marketplaceId + " listening for client orders on port " + clientPort);

        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                String orderJson = socket.recvStr(ZMQ.DONTWAIT);
                if (orderJson != null) {
                    executorService.submit(() -> processOrder(orderJson, socket));
                }
                Thread.sleep(10); // Small delay to prevent busy waiting
            } catch (Exception e) {
                System.err.println("Error handling client request: " + e.getMessage());
            }
        }

        socket.close();
    }

    private void processOrder(String orderJson, ZMQ.Socket responseSocket) {
        try {
            System.out.println("Marketplace " + marketplaceId + " received order: " + orderJson);

            // Parse multi-product order
            MultiProductOrder order = parseOrder(orderJson);

            // Execute 2-phase commit SAGA
            boolean success = sagaCoordinator.executeOrder(order);

            // Send response back to client
            String response = success ? "ORDER_COMPLETED" : "ORDER_FAILED";
            responseSocket.send(response);

        } catch (Exception e) {
            System.err.println("Error processing order: " + e.getMessage());
            responseSocket.send("ORDER_ERROR");
        }
    }

    private MultiProductOrder parseOrder(String orderJson) {
        // Minimal JSON parser for expected format
        MultiProductOrder order = new MultiProductOrder();
        String customerId = extractValue(orderJson, "customerId");
        if (customerId != null) {
            // Use constructor with customerId if available
            order = new MultiProductOrder(customerId);
        }
        String productsSection = extractArraySection(orderJson, "products");
        if (productsSection != null) {
            List<String> productJsons = splitProducts(productsSection);
            for (String productJson : productJsons) {
                String name = extractValue(productJson, "name");
                int quantity = parseIntOrDefault(extractValue(productJson, "quantity"), 1);
                String category = extractValue(productJson, "category");
                if (category == null) category = "general";
                double price = parseDoubleOrDefault(extractValue(productJson, "price"), 0.0);
                // sellerId can be extracted if needed: String sellerId = extractValue(productJson, "sellerId");
                order.addProduct(new Product(name, quantity, category, price));
            }
        }
        return order;
    }

    // Helper to extract a value for a key from a JSON-like string
    private String extractValue(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int idx = json.indexOf(pattern);
        if (idx == -1) return null;
        int start = idx + pattern.length();
        int end = json.indexOf('"', start);
        if (start == -1 || end == -1) return null;
        return json.substring(start, end);
    }

    private String extractArraySection(String json, String key) {
        String pattern = "\"" + key + "\":[";
        int idx = json.indexOf(pattern);
        if (idx == -1) return null;
        int start = json.indexOf('[', idx) + 1;
        int end = json.indexOf(']', start);
        if (start == 0 || end == -1) return null;
        return json.substring(start, end);
    }

    // Helper to split product objects in the array (assumes no nested objects)
    private List<String> splitProducts(String productsSection) {
        List<String> products = new ArrayList<>();
        int brace = 0, start = -1;
        for (int i = 0; i < productsSection.length(); i++) {
            char c = productsSection.charAt(i);
            if (c == '{') {
                if (brace == 0) start = i;
                brace++;
            } else if (c == '}') {
                brace--;
                if (brace == 0 && start != -1) {
                    products.add(productsSection.substring(start, i + 1));
                    start = -1;
                }
            }
        }
        return products;
    }

    private int parseIntOrDefault(String s, int def) {
        try { return s == null ? def : Integer.parseInt(s); } catch (Exception e) { return def; }
    }
    private double parseDoubleOrDefault(String s, double def) {
        try { return s == null ? def : Double.parseDouble(s); } catch (Exception e) { return def; }
    }

    private void shutdown() {
        running = false;
        executorService.shutdown();
        MessageUtils.context.close();
        System.out.println("Marketplace " + marketplaceId + " shutdown complete");
    }
}
