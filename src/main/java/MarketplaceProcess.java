// Main entry point for a single marketplace process
// This allows running a marketplace as a separate process
import marketplace.Marketplace;
import java.util.List;
import java.util.Arrays;

public class MarketplaceProcess {
    public static void main(String[] args) throws InterruptedException {
        // Default configuration - can be overridden by command line arguments
        String marketplacePort = "7777"; // Default port for this marketplace
        List<String> sellerEndpoints = List.of(
            "tcp://localhost:5555", 
            "tcp://localhost:5556",
            "tcp://localhost:5557",
            "tcp://localhost:5558",
            "tcp://localhost:5559"
        );
        
        // Parse command line arguments if provided
        if (args.length > 0) {
            marketplacePort = args[0];
        }
        if (args.length > 1) {
            // Parse seller endpoints from command line
            sellerEndpoints = Arrays.asList(args[1].split(","));
        }
        
        String processName = "Marketplace-" + marketplacePort;
        System.out.println("Starting Marketplace Process on port " + marketplacePort);
        System.out.println("Connecting to sellers: " + sellerEndpoints);
        
        // Initialize process monitoring
        ProcessMonitor.logProcessStart(processName, "port:" + marketplacePort);
        
        // Create marketplace instance
        Marketplace marketplace = new Marketplace(sellerEndpoints);
        
        // Simulate placing orders with performance monitoring
        for (int i = 0; i < 5; i++) {
            String orderId = "ORDER-" + marketplacePort + "-" + (i + 1);
            String product = "product" + (i % 3);
            
            System.out.println("\n--- Placing order " + (i + 1) + " ---");
            
            // Start monitoring this order
            ProcessMonitor.logOrderStart(processName, orderId, product);
            long startTime = System.currentTimeMillis();
            
            // Place the order
            marketplace.placeOrder(product);
            
            // Calculate processing time and log result
            long processingTime = System.currentTimeMillis() - startTime;
            // Note: We don't know if it succeeded from here, so we'll log it as processed
            ProcessMonitor.logOrderSuccess(processName, orderId, processingTime);
            
            Thread.sleep(2000); // Wait 2 seconds between orders
        }
        
        System.out.println("Marketplace process completed.");
        ProcessMonitor.printFinalStats();
    }
}
