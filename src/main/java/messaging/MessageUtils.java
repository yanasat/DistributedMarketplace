package messaging;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class MessageUtils {
    private static final ZContext context = new ZContext();

    public static ZMQ.Socket createSocket(String type, boolean bind, String endpoint) {
        ZMQ.Socket socket;
        if ("REQ".equals(type)) {
            socket = context.createSocket(ZMQ.REQ);
        } else if ("REP".equals(type)) {
            socket = context.createSocket(ZMQ.REP);
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type);
        }

        if (bind) {
            socket.bind(endpoint);
        } else {
            socket.connect(endpoint);
        }
        return socket;
    }
}
