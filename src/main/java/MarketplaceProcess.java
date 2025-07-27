import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import marketplace.Marketplace;

public class MarketplaceProcess {
    private static final Logger LOGGER = Logger.getLogger(MarketplaceProcess.class.getName());

    public static void main(String[] args) throws InterruptedException {
        // Default configuration - can be overridden by command line arguments
        final String marketplacePort;
        final List<String> sellerEndpoints;

        if (args.length > 0) {
            marketplacePort = args[0];
        } else {
            marketplacePort = "7777"; // Default port for this marketplace
        }

        if (args.length > 1) {
            sellerEndpoints = Arrays.asList(args[1].split(","));
        } else {
            sellerEndpoints = List.of(
                "tcp://localhost:5555", 
                "tcp://localhost:5556",
                "tcp://localhost:5557",
                "tcp://localhost:5558",
                "tcp://localhost:5559"
            );
        }

        final String processName = "Marketplace-" + marketplacePort;
        LOGGER.info(() -> String.format("Starting Marketplace Process on port %s", marketplacePort));
        LOGGER.info(() -> String.format("Connecting to sellers: %s", sellerEndpoints));

        // Initialize process monitoring
        ProcessMonitor.logProcessStart(processName, "port:" + marketplacePort);

        // Create marketplace instance
        final Marketplace marketplace = new Marketplace(sellerEndpoints);

        // Add shutdown hook to clean up
        Runtime.getRuntime().addShutdownHook(new Thread(() -> LOGGER.info("Shutting down Marketplace process...")));

        // Simulate placing orders with performance monitoring
        for (int i = 0; i < 5; i++) {
            final int orderIndex = i;
            final String orderId = "ORDER-" + marketplacePort + "-" + (orderIndex + 1);
            final String product = "product" + (orderIndex % 3);

            LOGGER.info(() -> String.format("%n--- Placing order %d ---", (orderIndex + 1)));

            // Start monitoring this order
            ProcessMonitor.logOrderStart(processName, orderId, product);
            long startTime = System.currentTimeMillis();

            try {
                // Place the order
                marketplace.placeOrder("Startwert", 1);

                // Calculate processing time and log result
                long processingTime = System.currentTimeMillis() - startTime;
                ProcessMonitor.logOrderSuccess(processName, orderId, processingTime);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error placing order", e);
                ProcessMonitor.logOrderFailure(processName, orderId, 0L); // Added default processing time
            }

            Thread.sleep(2000); // Wait 2 seconds between orders
        }

        LOGGER.info("Marketplace process completed.");
        ProcessMonitor.printFinalStats();
    }
}
