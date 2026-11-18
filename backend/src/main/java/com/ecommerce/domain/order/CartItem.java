package com.ecommerce.domain.order;

import com.ecommerce.domain.product.Product;
import com.fasterxml.jackson.annotation.JsonGetter;

/** One product in a cart. It holds no price: the line follows the product's current price until checkout. */
public class CartItem {
    private long cartItemId;
    private Product product;
    private int quantity;
    private String addedAt;

    public CartItem(long cartItemId, Product product, int quantity, String addedAt) {
        this.cartItemId = cartItemId;
        this.product = product;
        this.quantity = quantity;
        this.addedAt = addedAt;
    }

    @JsonGetter("lineAmount")
    public long lineAmount() {
        return product.getPrice() * quantity;
    }

    void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public long getCartItemId() { return cartItemId; }
    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public String getAddedAt() { return addedAt; }
}
