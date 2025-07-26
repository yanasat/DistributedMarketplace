package marketplace;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.zeromq.ZMQ;

import messaging.MessageUtils;
import model.Order;
import model.Order.Status;

public class Marketplace {
    private final List<String> sellerEndpoints;
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    public Marketplace(List<String> sellerEndpoints) {
        this.sellerEndpoints = sellerEndpoints;
    }

public void placeOrder(String product) {
    int quantity = 1; // TEMPORARY FIX: hardcoded quantity
            Order order = new Order(product, quantity);

        List<Future<Boolean>> futures = sellerEndpoints.stream()
                .map(endpoint -> executor.submit(() -> reserve(endpoint, order)))
                .collect(Collectors.toList());

        for (int i = 0; i < futures.size(); i++) {
            try {
                boolean confirmed = futures.get(i).get(2, TimeUnit.SECONDS);
                String endpoint = sellerEndpoints.get(i);
                if (confirmed) {
                    order.setStatus(endpoint, Status.CONFIRMED);
                } else {
                    order.setStatus(endpoint, Status.REJECTED);
                }
            } catch (Exception e) {
                System.out.println("Timeout/Error for seller " + sellerEndpoints.get(i));
                order.setStatus(sellerEndpoints.get(i), Status.REJECTED);
            }
        }

        if (order.isFullyConfirmed()) {
            System.out.println("Order CONFIRMED by all. Sending COMMIT...");
            sellerEndpoints.forEach(endpoint -> commit(endpoint, order));
        } else {
            System.out.println("One or more REJECTED. Rolling back...");
            rollback(order);
        }

        executor.shutdown();
    }

    private boolean reserve(String endpoint, Order order) {
        try (ZMQ.Socket socket = MessageUtils.createSocket("REQ", false, endpoint)) {
            String msg = String.format("RESERVE:%s:%s:%d", order.getId(), order.getProduct(), order.getQuantity());
            socket.send(msg);
            String reply = socket.recvStr();
            System.out.println("RESERVE response from " + endpoint + ": " + reply);
            return reply.startsWith("CONFIRMED");
        }
    }

    private void commit(String endpoint, Order order) {
        try (ZMQ.Socket socket = MessageUtils.createSocket("REQ", false, endpoint)) {
            String msg = String.format("COMMIT:%s:%s:%d", order.getId(), order.getProduct(), order.getQuantity());
            socket.send(msg);
            String reply = socket.recvStr();
            System.out.println("COMMIT response from " + endpoint + ": " + reply);
        }
    }

    private void rollback(Order order) {
        order.getSellerStatus().forEach((endpoint, status) -> {
            if (status == Status.CONFIRMED) {
                try (ZMQ.Socket socket = MessageUtils.createSocket("REQ", false, endpoint)) {
                    String msg = String.format("CANCEL:%s:%s:%d", order.getId(), order.getProduct(), order.getQuantity());
                    socket.send(msg);
                    String reply = socket.recvStr();
                    System.out.println("ROLLBACK response from " + endpoint + ": " + reply);
                }
            }
        });
    }
}
