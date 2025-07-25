package seller;

public class Reservation {
    public final String orderId;
    public final String productId;
    public final int quantity;

    public Reservation(String orderId, String productId, int quantity) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
    }
}
