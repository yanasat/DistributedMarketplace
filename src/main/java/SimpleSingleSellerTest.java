// Simple integration test for testing with just one seller
import java.util.List;
import java.util.logging.Logger;

import marketplace.Marketplace;
public class SimpleSingleSellerTest {
    private static final Logger logger = Logger.getLogger(SimpleSingleSellerTest.class.getName());

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Simple Test: Single Seller Communication ===");
        
        // Test with just one seller (the one you have running)
        List<String> sellerEndpoints = List.of("tcp://localhost:5555");
        
        logger.info("Testing connection to seller: " + sellerEndpoints);
        
        // Create marketplace instance
        Marketplace marketplace = new Marketplace(sellerEndpoints);
        
        // Test: Place a few orders
        for (int i = 0; i < 3; i++) {
            System.out.println("\n--- Test Order " + (i + 1) + " ---");
            marketplace.placeOrder("testProduct" + i);
            Thread.sleep(1000);
        }
        
        System.out.println("\n=== Simple Test Complete ===");
    }
}

