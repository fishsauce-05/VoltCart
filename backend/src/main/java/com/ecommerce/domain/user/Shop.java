package com.ecommerce.domain.user;

/** A seller's storefront. Only an ACTIVE shop can sell (BR01). */
public class Shop {
    public static final String ACTIVE = "ACTIVE";

    private long shopId;
    private Seller seller;
    private String shopName;
    private String pickupAddress;
    private String status;

    public Shop(long shopId, Seller seller, String shopName, String pickupAddress, String status) {
        this.shopId = shopId;
        this.seller = seller;
        this.shopName = shopName;
        this.pickupAddress = pickupAddress;
        this.status = status;
    }

    public boolean isActive() {
        return ACTIVE.equals(status);
    }

    public long getShopId() { return shopId; }
    public Seller getSeller() { return seller; }
    public String getShopName() { return shopName; }
    public String getPickupAddress() { return pickupAddress; }
    public String getStatus() { return status; }
}
