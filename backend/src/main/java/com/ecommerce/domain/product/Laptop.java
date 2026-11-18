package com.ecommerce.domain.product;

import com.ecommerce.domain.user.Shop;

public class Laptop extends Product {
    private String cpu;
    private int ramGb;
    private int storageGb;
    private double screenInch;
    private double weightKg;

    public Laptop(long productId, Shop shop, String sku, String name, String brand, String description, long price,
                  int availableStock, int committedStock, String imageUrl, String status,
                  String cpu, int ramGb, int storageGb, double screenInch, double weightKg) {
        super(productId, shop, sku, name, brand, "LAPTOP", description, price, availableStock, committedStock,
              imageUrl, status);
        this.cpu = cpu;
        this.ramGb = ramGb;
        this.storageGb = storageGb;
        this.screenInch = screenInch;
        this.weightKg = weightKg;
    }

    @Override
    public String specSummary() {
        return String.format("Laptop, %s, %d GB RAM, %d GB SSD, %.1f-inch screen, %.2f kg",
                cpu, ramGb, storageGb, screenInch, weightKg);
    }

    public String getCpu() { return cpu; }
    public int getRamGb() { return ramGb; }
    public int getStorageGb() { return storageGb; }
    public double getScreenInch() { return screenInch; }
    public double getWeightKg() { return weightKg; }
}
