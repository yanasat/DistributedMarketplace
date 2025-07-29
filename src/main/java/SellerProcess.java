// Enhanced Seller Process with configuration support
import seller.SellerStub;
import seller.SellerConfig;

public class SellerProcess {
    public static void main(String[] args) {
        // Default configuration - can be overridden by command line arguments
        String sellerEndpoint = "tcp://127.0.0.1:5555"; // Default endpoint
        SellerConfig config = null;
        
        // Parse command line arguments
        if (args.length > 0) {
            sellerEndpoint = args[0];
        }
        
        if (args.length > 1) {
            // Load configuration from YAML file
            try {
                config = SellerConfig.load(args[1]);
                System.out.println("Loaded configuration from: " + args[1]);
            } catch (Exception e) {
                System.out.println("Warning: Could not load config file " + args[1] + ", using defaults");
                System.out.println("Error: " + e.getMessage());
            }
        }
        
        // Make variables final for lambda usage
        final String finalSellerEndpoint = sellerEndpoint;
        final SellerConfig finalConfig = config;
        
        System.out.println("Starting Seller Process at " + finalSellerEndpoint);
        if (finalConfig != null) {
            System.out.println("Configuration: " + finalConfig.toString());
        }
        
        // Initialize process monitoring
        ProcessMonitor.logProcessStart("Seller-" + finalSellerEndpoint, finalSellerEndpoint);
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down Seller process at " + finalSellerEndpoint);
            SellerStub.stop();
        }));
        
        // Start the seller - this will run indefinitely
        SellerStub.start(finalSellerEndpoint, finalConfig);
    }
}