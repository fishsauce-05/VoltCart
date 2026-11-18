package com.ecommerce.domain.order;

import com.fasterxml.jackson.annotation.JsonGetter;

/** A line of an order, a fixed copy of the product as it was sold. */
public class OrderItem {
    private long orderItemId;
    private long productId;
    private String sku;
    private String productName;
    private long unitPrice;
    private int quantity;

    public OrderItem(long orderItemId, long productId, String sku, String productName, long unitPrice, int quantity) {
        this.orderItemId = orderItemId;
        this.productId = productId;
        this.sku = sku;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    @JsonGetter("lineAmount")
    public long lineAmount() {
        return unitPrice * quantity;
    }

    public long getOrderItemId() { return orderItemId; }
    public long getProductId() { return productId; }
    public String getSku() { return sku; }
    public String getProductName() { return productName; }
    public long getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
}
