package com.ecommerce.domain.product;

import com.ecommerce.domain.user.Shop;

public class BluetoothHeadphones extends Product {
    private String bluetoothVersion;
    private int batteryHours;
    private boolean noiseCancelling;
    private String formFactor;

    public BluetoothHeadphones(long productId, Shop shop, String sku, String name, String brand, String description,
                               long price, int availableStock, int committedStock, String imageUrl, String status,
                               String bluetoothVersion, int batteryHours, boolean noiseCancelling, String formFactor) {
        super(productId, shop, sku, name, brand, "HEADPHONES", description, price, availableStock, committedStock,
              imageUrl, status);
        this.bluetoothVersion = bluetoothVersion;
        this.batteryHours = batteryHours;
        this.noiseCancelling = noiseCancelling;
        this.formFactor = formFactor;
    }

    @Override
    public String specSummary() {
        String form = switch (formFactor) {
            case "OVER_EAR" -> "over-ear wireless headphones";
            case "ON_EAR" -> "on-ear wireless headphones";
            default -> "true wireless in-ear earbuds";
        };
        return String.format("Bluetooth %s, %s, %d hours battery%s", bluetoothVersion, form, batteryHours,
                noiseCancelling ? ", active noise cancelling" : "");
    }

    public String getBluetoothVersion() { return bluetoothVersion; }
    public int getBatteryHours() { return batteryHours; }
    public boolean isNoiseCancelling() { return noiseCancelling; }
    public String getFormFactor() { return formFactor; }
}
