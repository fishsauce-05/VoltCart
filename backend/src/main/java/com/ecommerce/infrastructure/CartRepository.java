package com.ecommerce.infrastructure;

import com.ecommerce.common.enums.CartStatus;
import com.ecommerce.common.infrastructure.BaseRepository;
import com.ecommerce.domain.order.Cart;
import com.ecommerce.domain.order.CartItem;
import com.ecommerce.domain.product.Product;
import com.ecommerce.domain.user.Customer;
import com.ecommerce.domain.user.UserRole;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Loads and saves a customer's active cart; the products of its items come from ProductRepository. */
@Repository
public class CartRepository extends BaseRepository {
    private final ProductRepository productRepository;

    public CartRepository(JdbcTemplate jdbc, ProductRepository productRepository) {
        super(jdbc);
        this.productRepository = productRepository;
    }

    public Cart findActiveByCustomer(long customerId) {
        Customer customer = customer(customerId);
        if (customer == null) {
            return null;
        }
        List<Long> carts = jdbc.queryForList("SELECT cartId FROM carts WHERE customerId = ? AND status = 'ACTIVE' "
                + "ORDER BY cartId DESC LIMIT 1", Long.class, customerId);
        if (carts.isEmpty()) {
            return null;
        }
        long cartId = carts.get(0);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT cartItemId, productId, quantity, addedAt FROM cartItems WHERE cartId = ? ORDER BY cartItemId",
                cartId);
        List<Long> ids = rows.stream().map(r -> ((Number) r.get("productId")).longValue()).toList();
        Map<Long, Product> products = productRepository.findByIds(ids).stream()
                .collect(Collectors.toMap(Product::getProductId, Function.identity()));
        List<CartItem> items = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Product product = products.get(((Number) r.get("productId")).longValue());
            items.add(new CartItem(((Number) r.get("cartItemId")).longValue(), product,
                    ((Number) r.get("quantity")).intValue(), String.valueOf(r.get("addedAt"))));
        }
        return new Cart(cartId, customer, CartStatus.ACTIVE, items);
    }

    /** Opens an empty cart; only an existing account with the CUSTOMER role may have one. */
    public Cart create(long customerId) {
        Customer customer = customer(customerId);
        if (customer == null) {
            throw new NoSuchElementException("Customer " + customerId + " does not exist");
        }
        if (!customer.canBuy()) {
            throw new IllegalStateException("Only a customer account can shop");
        }
        long id = insertAndGetId("INSERT INTO carts (customerId, status) VALUES (?, 'ACTIVE')", customerId);
        return new Cart(id, customer, CartStatus.ACTIVE, List.of());
    }

    /** Writes the cart's status and makes the stored items equal to the cart's items. */
    public void save(Cart cart) {
        jdbc.update("UPDATE carts SET status = ? WHERE cartId = ?", cart.getStatus().name(), cart.getCartId());
        List<Long> kept = new ArrayList<>();
        for (CartItem item : cart.getItems()) {
            if (item.getCartItemId() == 0) {
                kept.add(insertAndGetId("INSERT INTO cartItems (cartId, productId, quantity, addedAt) VALUES (?, ?, ?, ?)",
                        cart.getCartId(), item.getProduct().getProductId(), item.getQuantity(), item.getAddedAt()));
            } else {
                jdbc.update("UPDATE cartItems SET quantity = ? WHERE cartItemId = ?", item.getQuantity(),
                        item.getCartItemId());
                kept.add(item.getCartItemId());
            }
        }
        List<Long> stored = jdbc.queryForList("SELECT cartItemId FROM cartItems WHERE cartId = ?", Long.class,
                cart.getCartId());
        for (Long id : stored) {
            if (!kept.contains(id)) {
                jdbc.update("DELETE FROM cartItems WHERE cartItemId = ?", id);
            }
        }
    }

    private Customer customer(long customerId) {
        List<Customer> found = jdbc.query("SELECT * FROM customers WHERE customerId = ?",
                (r, i) -> new Customer(r.getLong("customerId"), r.getString("fullName"), r.getString("email"),
                        r.getString("phone"), r.getString("defaultAddress"), UserRole.valueOf(r.getString("role"))),
                customerId);
        return found.isEmpty() ? null : found.get(0);
    }
}
