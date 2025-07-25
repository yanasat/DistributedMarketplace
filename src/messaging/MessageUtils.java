package messaging;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import java.util.concurrent.atomic.AtomicReference;

// Enhanced MessageUtils with proper socket cleanup and port management
public class MessageUtils {
    // Shared ZeroMQ context with proper cleanup
    private static final AtomicReference<ZContext> contextRef = new AtomicReference<>();
    
    // Get or create ZeroMQ context
    public static ZContext getContext() {
        ZContext context = contextRef.get();
        if (context == null) {
            context = new ZContext();
            if (contextRef.compareAndSet(null, context)) {
                // Add shutdown hook for cleanup
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    ZContext ctx = contextRef.get();
                    if (ctx != null) {
                        System.out.println("🧹 Cleaning up ZeroMQ context...");
                        ctx.close();
                    }
                }));
                System.out.println("✅ ZeroMQ context initialized");
            } else {
                // Another thread created it first, use that one
                context.close();
                context = contextRef.get();
            }
        }
        return context;
    }
    
    // Create a ZeroMQ socket with enhanced error handling and cleanup
    public static ZMQ.Socket createSocket(String type, boolean bind, String endpoint) {
        ZContext context = getContext();
        ZMQ.Socket socket = null;
        
        try {
            // Create socket based on type
            if ("REQ".equals(type)) {
                socket = context.createSocket(ZMQ.REQ);
            } else if ("REP".equals(type)) {
                socket = context.createSocket(ZMQ.REP);
            } else {
                throw new IllegalArgumentException("Unsupported socket type: " + type);
            }
            
            // Configure socket options for better reliability
            socket.setReceiveTimeOut(5000);  // 5 second receive timeout
            socket.setSendTimeOut(5000);     // 5 second send timeout
            socket.setLinger(0);             // Don't wait on close
            socket.setReconnectIVL(1000);    // Reconnect interval: 1 second
            socket.setReconnectIVLMax(5000); // Max reconnect interval: 5 seconds
            
            if (bind) {
                // Enhanced binding with retry logic
                bindWithRetry(socket, endpoint, 3);
            } else {
                // Enhanced connection
                connectSafely(socket, endpoint);
            }
            
            return socket;
            
        } catch (Exception e) {
            // Cleanup on error
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception closeError) {
                    System.err.println("❌ Error closing socket during cleanup: " + closeError.getMessage());
                }
            }
            throw new RuntimeException("Failed to create socket: " + e.getMessage(), e);
        }
    }
    
    // Enhanced binding with retry and better error messages
    private static void bindWithRetry(ZMQ.Socket socket, String endpoint, int maxRetries) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                socket.bind(endpoint);
                System.out.println("✅ Successfully bound to " + endpoint + 
                    (attempt > 1 ? " (attempt " + attempt + ")" : ""));
                return;
                
            } catch (org.zeromq.ZMQException e) {
                lastException = e;
                
                if (e.getErrorCode() == 48) { // EADDRINUSE
                    System.out.println("⚠️  Port " + extractPort(endpoint) + " is busy (attempt " + attempt + "/" + maxRetries + ")");
                    
                    if (attempt < maxRetries) {
                        // Try alternative port
                        String newEndpoint = incrementPort(endpoint);
                        System.out.println("🔄 Trying alternative: " + newEndpoint);
                        endpoint = newEndpoint;
                        
                        try {
                            Thread.sleep(1000); // Wait 1 second before retry
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Interrupted during retry", ie);
                        }
                    }
                } else {
                    // Other error, don't retry
                    break;
                }
            }
        }
        
        // All retries failed
        throw new RuntimeException("Failed to bind to " + endpoint + " after " + maxRetries + " attempts. " +
            "Last error: " + (lastException != null ? lastException.getMessage() : "unknown") + 
            "\nTry using a different port or stop other processes using this port.", lastException);
    }
    
    // Safe connection with better error handling
    private static void connectSafely(ZMQ.Socket socket, String endpoint) {
        try {
            socket.connect(endpoint);
            System.out.println("🔗 Connected to " + endpoint);
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to " + endpoint + ": " + e.getMessage(), e);
        }
    }
    
    // Extract port number from endpoint
    private static String extractPort(String endpoint) {
        if (endpoint.contains(":")) {
            String[] parts = endpoint.split(":");
            return parts[parts.length - 1];
        }
        return "unknown";
    }
    
    // Increment port number for retry
    private static String incrementPort(String endpoint) {
        try {
            if (endpoint.startsWith("tcp://localhost:")) {
                String portStr = endpoint.substring("tcp://localhost:".length());
                int port = Integer.parseInt(portStr);
                return "tcp://localhost:" + (port + 1);
            }
        } catch (NumberFormatException e) {
            // If we can't parse, just return original
        }
        return endpoint;
    }
    
    // Utility method to properly close socket
    public static void closeSocket(ZMQ.Socket socket) {
        if (socket != null) {
            try {
                socket.close();
                System.out.println("🔌 Socket closed");
            } catch (Exception e) {
                System.err.println("⚠️  Error closing socket: " + e.getMessage());
            }
        }
    }
    
    // Clean shutdown of entire ZeroMQ context
    public static void shutdown() {
        ZContext context = contextRef.getAndSet(null);
        if (context != null) {
            System.out.println("🧹 Shutting down ZeroMQ context...");
            context.close();
            System.out.println("✅ ZeroMQ context shut down");
        }
    }
    
    // Health check method
    public static boolean isEndpointAvailable(String endpoint) {
        ZMQ.Socket testSocket = null;
        try {
            testSocket = createSocket("REQ", false, endpoint);
            testSocket.setReceiveTimeOut(1000); // Short timeout for testing
            testSocket.send("PING");
            String response = testSocket.recvStr();
            return response != null;
        } catch (Exception e) {
            return false;
        } finally {
            closeSocket(testSocket);
        }
    }
}
