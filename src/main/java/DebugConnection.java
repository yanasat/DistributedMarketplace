// Debug tool to test ZeroMQ connection step by step
import messaging.MessageUtils;
import org.zeromq.ZMQ;

public class DebugConnection {
    public static void main(String[] args) {
        System.out.println("=== ZeroMQ Connection Debug Tool ===");
        
        // Test each seller endpoint one by one
        String[] endpoints = {
            "tcp://localhost:5555",
            "tcp://localhost:5556", 
            "tcp://localhost:5557",
            "tcp://localhost:5558",
            "tcp://localhost:5559"
        };
        
        for (String endpoint : endpoints) {
            System.out.println("\nTesting connection to: " + endpoint);
            testConnection(endpoint);
        }
        
        System.out.println("\n=== Debug Complete ===");
    }
    
    private static void testConnection(String endpoint) {
        ZMQ.Socket socket = null;
        try {
            // Create socket with timeout
            socket = MessageUtils.createSocket("REQ", false, endpoint);
            socket.setReceiveTimeOut(2000); // 2 second timeout
            socket.setSendTimeOut(2000);    // 2 second timeout
            
            System.out.println("  Socket created successfully");
            
            // Send test message
            System.out.println("  Sending test message...");
            boolean sent = socket.send("ORDER:debugTest");
            if (!sent) {
                System.out.println("  ❌ Failed to send message");
                return;
            }
            System.out.println("  ✅ Message sent");
            
            // Try to receive response
            System.out.println("  Waiting for response...");
            String response = socket.recvStr();
            if (response != null) {
                System.out.println("  ✅ Response received: " + response);
            } else {
                System.out.println("  ❌ No response (timeout)");
            }
            
        } catch (Exception e) {
            System.out.println("  ❌ Exception: " + e.getMessage());
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }
}
