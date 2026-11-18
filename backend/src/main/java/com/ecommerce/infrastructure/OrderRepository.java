package com.ecommerce.infrastructure;

import com.ecommerce.common.infrastructure.BaseRepository;
import com.ecommerce.domain.order.Order;
import com.ecommerce.domain.order.OrderItem;
import com.ecommerce.domain.order.Payment;
import com.ecommerce.domain.user.Customer;
import com.ecommerce.domain.user.Seller;
import com.ecommerce.domain.user.Shop;
import com.ecommerce.domain.user.UserRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Saves an order with its items and payment, and reads it back with its customer and shop. */
@Repository
public class OrderRepository extends BaseRepository {

    public OrderRepository(JdbcTemplate jdbc) {
        super(jdbc);
    }

    /** Inserts the order, its items and its payment; the caller's transaction keeps them together. */
    public Order save(Order order) {
        long orderId = insertAndGetId("INSERT INTO orders (customerId, shopId, status, deliveryAddress, totalAmount, "
                        + "requestReference, createdAt) VALUES (?, ?, ?, ?, ?, ?, ?)",
                order.getCustomer().getCustomerId(), order.getShop().getShopId(), order.getStatus(),
                order.getDeliveryAddress(), order.calculateTotal(), order.getRequestReference(),
                LocalDateTime.now().withNano(0).toString());
        for (OrderItem item : order.getItems()) {
            jdbc.update("INSERT INTO orderItems (orderId, productId, sku, productName, unitPrice, quantity) "
                            + "VALUES (?, ?, ?, ?, ?, ?)", orderId, item.getProductId(), item.getSku(),
                    item.getProductName(), item.getUnitPrice(), item.getQuantity());
        }
        Payment p = order.getPayment();
        if (p != null) {
            jdbc.update("INSERT INTO payments (orderId, method, amount, status, reference) VALUES (?, ?, ?, ?, ?)",
                    orderId, p.getMethod(), p.getAmount(), p.getStatus(), p.getReference());
        }
        return findById(orderId);
    }

    public Order findById(long orderId) {
        // the header is read completely before the items and the payment: with one pooled connection,
        // a query started inside another query's row mapper would wait for a connection that never frees
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT o.*, c.fullName, c.email, c.phone, c.defaultAddress, c.role,
                       s.shopName, s.pickupAddress, s.status AS shopStatus,
                       se.sellerId, se.fullName AS sellerName, se.email AS sellerEmail
                FROM orders o
                JOIN customers c ON c.customerId = o.customerId
                JOIN shops s ON s.shopId = o.shopId
                JOIN sellers se ON se.sellerId = s.sellerId
                WHERE o.orderId = ?""", orderId);
        if (rows.isEmpty()) {
            return null;
        }
        Map<String, Object> r = rows.get(0);
        Customer customer = new Customer(number(r, "customerId"), text(r, "fullName"), text(r, "email"),
                text(r, "phone"), text(r, "defaultAddress"), UserRole.valueOf(text(r, "role")));
        Shop shop = new Shop(number(r, "shopId"), new Seller(number(r, "sellerId"), text(r, "sellerName"),
                text(r, "sellerEmail"), UserRole.SELLER), text(r, "shopName"), text(r, "pickupAddress"),
                text(r, "shopStatus"));
        return new Order(orderId, customer, shop, text(r, "status"), text(r, "deliveryAddress"),
                number(r, "totalAmount"), text(r, "requestReference"), text(r, "createdAt"), items(orderId),
                payment(orderId));
    }

    /** The order already created for this request, so a repeated submission creates no second order (BR10). */
    public Order findByRequestReference(String requestReference) {
        List<Long> ids = jdbc.queryForList("SELECT orderId FROM orders WHERE requestReference = ?", Long.class,
                requestReference);
        return ids.isEmpty() ? null : findById(ids.get(0));
    }

    private static long number(Map<String, Object> row, String column) {
        return ((Number) row.get(column)).longValue();
    }

    private static String text(Map<String, Object> row, String column) {
        Object value = row.get(column);
        return value == null ? null : value.toString();
    }

    private List<OrderItem> items(long orderId) {
        return jdbc.query("SELECT * FROM orderItems WHERE orderId = ? ORDER BY orderItemId",
                (r, i) -> new OrderItem(r.getLong("orderItemId"), r.getLong("productId"), r.getString("sku"),
                        r.getString("productName"), r.getLong("unitPrice"), r.getInt("quantity")), orderId);
    }

    private Payment payment(long orderId) {
        List<Payment> found = jdbc.query("SELECT * FROM payments WHERE orderId = ?",
                (r, i) -> new Payment(r.getLong("paymentId"), r.getString("method"), r.getLong("amount"),
                        r.getString("status"), r.getString("reference")), orderId);
        return found.isEmpty() ? null : found.get(0);
    }
}
