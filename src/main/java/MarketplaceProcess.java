import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import marketplace.Marketplace;
import marketplace.MarketplaceConfig;

public class MarketplaceProcess {
    private static final Logger LOGGER = Logger.getLogger(MarketplaceProcess.class.getName());

    public static void main(String[] args) throws InterruptedException {
        String configFile = args.length > 0 ? args[0] : "src/main/resources/marketplace.yaml";
        MarketplaceConfig config = MarketplaceConfig.load(configFile);
        
        final String marketplacePort = String.valueOf(config.marketplace.port);
        final String processName = config.marketplace.name;

        LOGGER.info(() -> String.format("Starting %s on port %s", processName, marketplacePort));
        LOGGER.info(() -> String.format("Configuration: %s", config.toString()));
        LOGGER.info(() -> String.format("Connecting to sellers: %s", config.sellers));

        ProcessMonitor.logProcessStart(processName, "port:" + marketplacePort);

        final Marketplace marketplace = new Marketplace(config.sellers, config.orders.timeout_ms, processName);
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down Marketplace process...");
            marketplace.stop();
        }));

        Random rand = new Random();
        
        for (int i = 0; i < config.orders.max_orders; i++) {
            final int orderIndex = i;
            final String orderId = "ORDER-" + marketplacePort + "-" + (orderIndex + 1);
            final String product = config.products.get(rand.nextInt(config.products.size()));
            final int quantity = rand.nextInt(3) + 1;

            LOGGER.info(() -> String.format("%n--- Placing order %d ---", (orderIndex + 1)));
            LOGGER.info(() -> String.format("Product: %s, Quantity: %d", product, quantity));

            ProcessMonitor.logOrderStart(processName, orderId, product);
            long startTime = System.currentTimeMillis();

            try {
                marketplace.placeOrder(product, quantity);

                long processingTime = System.currentTimeMillis() - startTime;
                ProcessMonitor.logOrderSuccess(processName, orderId, processingTime);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error placing order", e);
                long processingTime = System.currentTimeMillis() - startTime;
                ProcessMonitor.logOrderFailure(processName, orderId, processingTime);
            }

            if (i < config.orders.max_orders - 1) {
                Thread.sleep(config.orders.arrival_rate_ms);
            }
        }

        LOGGER.info("Marketplace process completed.");
        ProcessMonitor.printFinalStats();
        

        marketplace.stop();
    }
}