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
        if ("REQ".equals(type)) socket = context.createSocket(ZMQ.REQ);
        else if ("REP".equals(type)) socket = context.createSocket(ZMQ.REP);
        else throw new IllegalArgumentException("Socket type not supported");

        if (bind) socket.bind(endpoint);
        else socket.connect(endpoint);
        return socket;
    }
}
