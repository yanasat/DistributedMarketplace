import seller.SellerStub;
import seller.SellerConfig;

public class SellerProcess {
    public static void main(String[] args) {
        String sellerEndpoint = "tcp://127.0.0.1:5555";
        SellerConfig config = null;
        
        if (args.length > 0) {
            sellerEndpoint = args[0];
        }
        
        if (args.length > 1) {
            try {
                config = SellerConfig.load(args[1]);
                System.out.println("Loaded configuration from: " + args[1]);
            } catch (Exception e) {
                System.out.println("Warning: Could not load config file " + args[1] + ", using defaults");
                System.out.println("Error: " + e.getMessage());
            }
        }
        
        final String finalSellerEndpoint = sellerEndpoint;
        final SellerConfig finalConfig = config;
        
        System.out.println("Starting Seller Process at " + finalSellerEndpoint);
        if (finalConfig != null) {
            System.out.println("Configuration: " + finalConfig.toString());
        }
        
        ProcessMonitor.logProcessStart("Seller-" + finalSellerEndpoint, finalSellerEndpoint);
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down Seller process at " + finalSellerEndpoint);
            SellerStub.stop();
        }));
        
        SellerStub.start(finalSellerEndpoint, finalConfig);
    }
}