package com.ecommerce.domain.order;

import com.ecommerce.common.enums.CartStatus;
import com.ecommerce.domain.product.Product;
import com.ecommerce.domain.user.Customer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * The customer's selection across shops. It reserves no stock (BR02): only checkout commits stock.
 * Items are grouped by shop because one checkout covers one shop (BR03).
 */
public class Cart {
    /** BR08: at most five units of one product per cart, so scarce devices are not hoarded. */
    public static final int MAX_QUANTITY_PER_ITEM = 5;

    private long cartId;
    private Customer customer;
    private CartStatus status;
    private List<CartItem> items;

    public Cart(long cartId, Customer customer, CartStatus status, List<CartItem> items) {
        this.cartId = cartId;
        this.customer = customer;
        this.status = status;
        this.items = new ArrayList<>(items);
    }

    public void addItem(Product product, int quantity) {
        requireActive();
        if (!product.isSearchable()) {
            throw new IllegalStateException(product.getName() + " is not on sale");
        }
        CartItem existing = find(product.getProductId());
        int total = quantity + (existing == null ? 0 : existing.getQuantity());
        checkQuantity(product, quantity, total);
        if (existing == null) {
            items.add(new CartItem(0, product, quantity, LocalDateTime.now().withNano(0).toString()));
        } else {
            existing.setQuantity(total);
        }
    }

    public void updateQuantity(long productId, int quantity) {
        requireActive();
        CartItem item = require(productId);
        checkQuantity(item.getProduct(), quantity, quantity);
        item.setQuantity(quantity);
    }

    public void removeItem(long productId) {
        requireActive();
        items.remove(require(productId));
    }

    public List<CartItem> itemsOfShop(long shopId) {
        return items.stream().filter(i -> i.getProduct().getShop().getShopId() == shopId).toList();
    }

    /** Sum of the items of one shop at the current prices of the products. */
    public long shopSubtotal(long shopId) {
        return itemsOfShop(shopId).stream().mapToLong(CartItem::lineAmount).sum();
    }

    /** After checkout only that shop's items leave the cart; an empty cart is closed. */
    public void removeShopItems(long shopId) {
        items.removeIf(i -> i.getProduct().getShop().getShopId() == shopId);
        if (items.isEmpty()) {
            status = CartStatus.CHECKED_OUT;
        }
    }

    private void checkQuantity(Product product, int added, int total) {
        if (added <= 0) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        if (total > MAX_QUANTITY_PER_ITEM) {
            throw new IllegalArgumentException("At most " + MAX_QUANTITY_PER_ITEM + " units of one product per cart");
        }
        if (total > product.getAvailableStock()) {
            throw new IllegalStateException("Only " + product.getAvailableStock() + " left of " + product.getName());
        }
    }

    private void requireActive() {
        if (status != CartStatus.ACTIVE) {
            throw new IllegalStateException("This cart is already checked out");
        }
    }

    private CartItem find(long productId) {
        return items.stream().filter(i -> i.getProduct().getProductId() == productId).findFirst().orElse(null);
    }

    private CartItem require(long productId) {
        CartItem item = find(productId);
        if (item == null) {
            throw new NoSuchElementException("Product " + productId + " is not in the cart");
        }
        return item;
    }

    public long getCartId() { return cartId; }
    public Customer getCustomer() { return customer; }
    public CartStatus getStatus() { return status; }
    public List<CartItem> getItems() { return List.copyOf(items); }
}
