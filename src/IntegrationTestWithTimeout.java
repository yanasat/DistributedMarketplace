// Integration test with timeouts to prevent hanging
import marketplace.Marketplace;
import messaging.MessageUtils;
import org.zeromq.ZMQ;
import java.util.List;

public class IntegrationTestWithTimeout {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Integration Test with Timeouts ===");
        
        // First, check if sellers are responding
        List<String> sellerEndpoints = List.of(
            "tcp://localhost:5555",
            "tcp://localhost:5556",
            "tcp://localhost:5557",
            "tcp://localhost:5558",
            "tcp://localhost:5559"
        );
        
        System.out.println("Checking seller availability...");
        for (String endpoint : sellerEndpoints) {
            if (isSellerResponding(endpoint)) {
                System.out.println("✅ " + endpoint + " is responding");
            } else {
                System.out.println("❌ " + endpoint + " is NOT responding");
            }
        }
        
        System.out.println("\nStarting marketplace test...");
        
        // Create marketplace instance  
        Marketplace marketplace = new Marketplace(sellerEndpoints);
        
        // Test with timeout protection
        System.out.println("\n--- Test: Single Order with Timeout ---");
        try {
            marketplace.placeOrder("testProduct");
            System.out.println("✅ Order completed successfully");
        } catch (Exception e) {
            System.out.println("❌ Order failed: " + e.getMessage());
        }
        
        System.out.println("\n=== Test Complete ===");
    }
    
    private static boolean isSellerResponding(String endpoint) {
        ZMQ.Socket socket = null;
        try {
            socket = MessageUtils.createSocket("REQ", false, endpoint);
            socket.setReceiveTimeOut(1000); // 1 second timeout
            socket.setSendTimeOut(1000);
            
            socket.send("ORDER:pingTest");
            String response = socket.recvStr();
            return response != null;
            
        } catch (Exception e) {
            return false;
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }
}
