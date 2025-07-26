package messaging;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;

// MessageUtils provides utility methods for ZeroMQ socket creation
public class MessageUtils {
    // Shared ZeroMQ context for all sockets
    public static ZContext context = new ZContext();

    // Create a ZeroMQ socket of the given type, binding or connecting to the endpoint
    public static ZMQ.Socket createSocket(String type, boolean bind, String endpoint) {
        ZMQ.Socket socket;
        switch (type) {
            case "REQ":
                socket = context.createSocket(ZMQ.REQ);
                break;
            case "REP":
                socket = context.createSocket(ZMQ.REP);
                break;
            case "PUB":
                socket = context.createSocket(ZMQ.PUB);
                break;
            case "SUB":
                socket = context.createSocket(ZMQ.SUB);
                socket.subscribe(""); // Subscribe to all messages
                break;
            default:
                throw new IllegalArgumentException("Unsupported socket type: " + type);
        }

        if (bind) {
            socket.bind(endpoint);
        } else {
            socket.connect(endpoint);
        }

        return socket;
    }
}
