package com.ecommerce.domain.product;

import com.ecommerce.domain.user.Shop;
import com.fasterxml.jackson.annotation.JsonGetter;

/**
 * A listing of one shop. Laptop, Phone and BluetoothHeadphones add the specifications of their category,
 * and each describes itself through specSummary().
 */
public abstract class Product {
    public static final String PUBLISHED = "PUBLISHED";

    private long productId;
    private Shop shop;
    private String sku;
    private String name;
    private String brand;
    private String category;
    private String description;
    private long price;
    private int availableStock;
    private int committedStock;
    private String imageUrl;
    private String status;

    protected Product(long productId, Shop shop, String sku, String name, String brand, String category,
                      String description, long price, int availableStock, int committedStock,
                      String imageUrl, String status) {
        this.productId = productId;
        this.shop = shop;
        this.sku = sku;
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.description = description;
        this.price = price;
        this.availableStock = availableStock;
        this.committedStock = committedStock;
        this.imageUrl = imageUrl;
        this.status = status;
    }

    /** BR01: a customer sees and buys only published products of an active shop. */
    public boolean isSearchable() {
        return PUBLISHED.equals(status) && shop != null && shop.isActive();
    }

    /** BR05: checkout moves the quantity from available to committed stock; stock never goes negative. */
    public void commitStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (quantity > availableStock) {
            throw new IllegalStateException("Only " + availableStock + " left of " + name);
        }
        availableStock -= quantity;
        committedStock += quantity;
    }

    /** The reverse of commitStock, used when an order is cancelled before handover. */
    public void releaseStock(int quantity) {
        if (quantity <= 0 || quantity > committedStock) {
            throw new IllegalArgumentException("Cannot release " + quantity + " units of " + name);
        }
        committedStock -= quantity;
        availableStock += quantity;
    }

    /** One line of the specifications that matter for this category, in words a customer would search with. */
    @JsonGetter("specSummary")
    public abstract String specSummary();

    /** The text from which the product's text vector is computed: name, brand, specifications and description. */
    public String indexText() {
        return name + ". " + brand + ". " + specSummary() + ". " + description;
    }

    public long getProductId() { return productId; }
    public Shop getShop() { return shop; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public String getBrand() { return brand; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public long getPrice() { return price; }
    public int getAvailableStock() { return availableStock; }
    public int getCommittedStock() { return committedStock; }
    public String getImageUrl() { return imageUrl; }
    public String getStatus() { return status; }
}
