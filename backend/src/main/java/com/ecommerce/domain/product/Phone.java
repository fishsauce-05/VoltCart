package com.ecommerce.domain.product;

import com.ecommerce.domain.user.Shop;

public class Phone extends Product {
    private String chipset;
    private int ramGb;
    private int storageGb;
    private double screenInch;
    private int batteryMah;

    public Phone(long productId, Shop shop, String sku, String name, String brand, String description, long price,
                 int availableStock, int committedStock, String imageUrl, String status,
                 String chipset, int ramGb, int storageGb, double screenInch, int batteryMah) {
        super(productId, shop, sku, name, brand, "PHONE", description, price, availableStock, committedStock,
              imageUrl, status);
        this.chipset = chipset;
        this.ramGb = ramGb;
        this.storageGb = storageGb;
        this.screenInch = screenInch;
        this.batteryMah = batteryMah;
    }

    @Override
    public String specSummary() {
        return String.format("Smartphone, %s chip, %d GB RAM, %d GB storage, %.2f-inch screen, %d mAh battery",
                chipset, ramGb, storageGb, screenInch, batteryMah);
    }

    public String getChipset() { return chipset; }
    public int getRamGb() { return ramGb; }
    public int getStorageGb() { return storageGb; }
    public double getScreenInch() { return screenInch; }
    public int getBatteryMah() { return batteryMah; }
}
