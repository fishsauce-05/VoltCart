package com.ecommerce.domain.order;

import com.ecommerce.domain.product.Product;
import com.ecommerce.domain.user.Customer;
import com.ecommerce.domain.user.Shop;
import java.util.ArrayList;
import java.util.List;

/** A purchase from one shop. Its items keep the price and name of the moment of checkout (BR04). */
public class Order {
    /** BR09: cash on delivery is refused above this total, because a refused high-value parcel is costly. */
    public static final long COD_LIMIT = 50_000_000L;

    private long orderId;
    private Customer customer;
    private Shop shop;
    private String status;
    private String deliveryAddress;
    private long totalAmount;
    private String requestReference;
    private String createdAt;
    private List<OrderItem> items;
    private Payment payment;

    public Order(long orderId, Customer customer, Shop shop, String status, String deliveryAddress, long totalAmount,
                 String requestReference, String createdAt, List<OrderItem> items, Payment payment) {
        this.orderId = orderId;
        this.customer = customer;
        this.shop = shop;
        this.status = status;
        this.deliveryAddress = deliveryAddress;
        this.totalAmount = totalAmount;
        this.requestReference = requestReference;
        this.createdAt = createdAt;
        this.items = new ArrayList<>(items);
        this.payment = payment;
    }

    /** Copies SKU, name and current price of the product into a new OrderItem (BR03, BR04). */
    public void addItem(Product product, int quantity) {
        if (product.getShop().getShopId() != shop.getShopId()) {
            throw new IllegalArgumentException(product.getName() + " is sold by another shop");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        items.add(new OrderItem(0, product.getProductId(), product.getSku(), product.getName(), product.getPrice(),
                quantity));
        totalAmount = calculateTotal();
    }

    public long calculateTotal() {
        return items.stream().mapToLong(OrderItem::lineAmount).sum();
    }

    /**
     * Creates the payment for the whole total. Cash on delivery places the order at once; a bank transfer
     * leaves it awaiting payment until the Payment Service confirms it (UC11, outside the prototype).
     */
    public Payment addPayment(String method) {
        if (items.isEmpty()) {
            throw new IllegalStateException("An order needs at least one item");
        }
        totalAmount = calculateTotal();
        if (Payment.COD.equals(method)) {
            if (totalAmount > COD_LIMIT) {
                throw new IllegalArgumentException("Cash on delivery is limited to 50,000,000 VND; choose bank transfer");
            }
            payment = new Payment(0, method, totalAmount, "PENDING_COLLECTION", null);
            status = "PLACED";
        } else if (Payment.BANK_TRANSFER.equals(method)) {
            payment = new Payment(0, method, totalAmount, "AWAITING_TRANSFER", null);
            status = "AWAITING_PAYMENT";
        } else {
            throw new IllegalArgumentException("Unknown payment method: " + method);
        }
        return payment;
    }

    public long getOrderId() { return orderId; }
    public Customer getCustomer() { return customer; }
    public Shop getShop() { return shop; }
    public String getStatus() { return status; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public long getTotalAmount() { return totalAmount; }
    public String getRequestReference() { return requestReference; }
    public String getCreatedAt() { return createdAt; }
    public List<OrderItem> getItems() { return List.copyOf(items); }
    public Payment getPayment() { return payment; }
}
