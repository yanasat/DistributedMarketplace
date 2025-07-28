// Main entry point for a single seller process
// This allows running a seller as a separate process
import seller.SellerStub;

public class SellerProcess {
    public static void main(String[] args) {
        // Default configuration - can be overridden by command line arguments
        String sellerEndpoint = "tcp://127.0.0.1:5555"; // Default endpoint
        
        // Parse command line arguments if provided
        if (args.length > 0) {
            sellerEndpoint = args[0];
        }
        
        System.out.println("Starting Seller Process at " + sellerEndpoint);
        
        // Initialize process monitoring
        ProcessMonitor.logProcessStart("Seller-" + sellerEndpoint, sellerEndpoint);
        
        // Start the seller - this will run indefinitely
        SellerStub.start(sellerEndpoint);
    }
}
