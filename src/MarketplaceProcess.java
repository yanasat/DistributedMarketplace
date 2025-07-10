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
        
        System.out.println("Starting Marketplace Process on port " + marketplacePort);
        System.out.println("Connecting to sellers: " + sellerEndpoints);
        
        // Create marketplace instance
        Marketplace marketplace = new Marketplace(sellerEndpoints);
        
        // Simulate placing orders (this will be replaced by Yana's implementation)
        for (int i = 0; i < 5; i++) {
            System.out.println("\n--- Placing order " + (i + 1) + " ---");
            marketplace.placeOrder("product" + (i % 3)); // Cycle through 3 different products
            Thread.sleep(2000); // Wait 2 seconds between orders
        }
        
        System.out.println("Marketplace process completed.");
    }
}
