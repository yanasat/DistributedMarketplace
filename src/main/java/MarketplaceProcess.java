import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import marketplace.Marketplace;
import marketplace.MarketplaceConfig;

public class MarketplaceProcess {
    private static final Logger LOGGER = Logger.getLogger(MarketplaceProcess.class.getName());

    public static void main(String[] args) throws InterruptedException {
        // Load configuration
        String configFile = args.length > 0 ? args[0] : "src/main/resources/marketplace.yaml";
        MarketplaceConfig config = MarketplaceConfig.load(configFile);
        
        final String marketplacePort = String.valueOf(config.marketplace.port);
        final String processName = config.marketplace.name;

        LOGGER.info(() -> String.format("Starting %s on port %s", processName, marketplacePort));
        LOGGER.info(() -> String.format("Configuration: %s", config.toString()));
        LOGGER.info(() -> String.format("Connecting to sellers: %s", config.sellers));

        // Initialize process monitoring
        ProcessMonitor.logProcessStart(processName, "port:" + marketplacePort);

        // Create marketplace instance with configured timeout
        final Marketplace marketplace = new Marketplace(config.sellers, config.orders.timeout_ms);

        // Add shutdown hook to clean up
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down Marketplace process...");
            marketplace.stop();
        }));

        // Generate random orders using configuration
        Random rand = new Random();
        
        for (int i = 0; i < config.orders.max_orders; i++) {
            final int orderIndex = i;
            final String orderId = "ORDER-" + marketplacePort + "-" + (orderIndex + 1);
            final String product = config.products.get(rand.nextInt(config.products.size()));
            final int quantity = rand.nextInt(3) + 1; // 1-3 items

            LOGGER.info(() -> String.format("%n--- Placing order %d ---", (orderIndex + 1)));
            LOGGER.info(() -> String.format("Product: %s, Quantity: %d", product, quantity));

            // Start monitoring this order
            ProcessMonitor.logOrderStart(processName, orderId, product);
            long startTime = System.currentTimeMillis();

            try {
                // Place the order
                marketplace.placeOrder(product, quantity);

                // Calculate processing time and log result
                long processingTime = System.currentTimeMillis() - startTime;
                ProcessMonitor.logOrderSuccess(processName, orderId, processingTime);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error placing order", e);
                long processingTime = System.currentTimeMillis() - startTime;
                ProcessMonitor.logOrderFailure(processName, orderId, processingTime);
            }

            // Wait between orders using configured arrival rate
            if (i < config.orders.max_orders - 1) {
                Thread.sleep(config.orders.arrival_rate_ms);
            }
        }

        LOGGER.info("Marketplace process completed.");
        ProcessMonitor.printFinalStats();
        
        // Graceful shutdown
        marketplace.stop();
    }
}