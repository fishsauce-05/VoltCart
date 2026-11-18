package com.ecommerce.infrastructure;

import com.ecommerce.common.infrastructure.BaseRepository;
import com.ecommerce.domain.product.BluetoothHeadphones;
import com.ecommerce.domain.product.Laptop;
import com.ecommerce.domain.product.Phone;
import com.ecommerce.domain.product.Product;
import com.ecommerce.domain.user.Seller;
import com.ecommerce.domain.user.Shop;
import com.ecommerce.domain.user.UserRole;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Loads products with their shop and the specifications of their category (one table per subclass,
 * joined on productId), and saves the stock that checkout changes.
 */
@Repository
public class ProductRepository extends BaseRepository {
    private static final String SELECT = """
            SELECT p.*, s.shopName, s.pickupAddress, s.status AS shopStatus,
                   se.sellerId, se.fullName AS sellerName, se.email AS sellerEmail,
                   l.cpu, l.ramGb AS lRam, l.storageGb AS lStorage, l.screenInch AS lScreen, l.weightKg,
                   ph.chipset, ph.ramGb AS pRam, ph.storageGb AS pStorage, ph.screenInch AS pScreen, ph.batteryMah,
                   b.bluetoothVersion, b.batteryHours, b.noiseCancelling, b.formFactor
            FROM products p
            JOIN shops s ON s.shopId = p.shopId
            JOIN sellers se ON se.sellerId = s.sellerId
            LEFT JOIN laptops l ON l.productId = p.productId
            LEFT JOIN phones ph ON ph.productId = p.productId
            LEFT JOIN bluetoothHeadphones b ON b.productId = p.productId
            """;
    private static final String SEARCHABLE = " p.status = 'PUBLISHED' AND s.status = 'ACTIVE' ";
    private static final String SEARCH_TEXT = """
             lower(p.name || ' ' || p.brand || ' ' || p.description || ' ' || coalesce(l.cpu, '') || ' ' ||
                   coalesce(ph.chipset, '') || ' ' || coalesce(b.formFactor, '') || ' ' ||
                   CASE p.category WHEN 'LAPTOP' THEN 'laptop notebook' WHEN 'PHONE' THEN 'phone smartphone'
                        ELSE 'headphones earbuds bluetooth' END) LIKE ?
            """;

    public ProductRepository(JdbcTemplate jdbc) {
        super(jdbc);
    }

    public Product findById(long productId) {
        List<Product> found = jdbc.query(SELECT + " WHERE p.productId = ?", this::map, productId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The products with these identifiers, in the order of the identifiers (the ranking order). */
    public List<Product> findByIds(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return List.of();
        }
        String marks = String.join(",", Collections.nCopies(productIds.size(), "?"));
        Map<Long, Product> byId = new HashMap<>();
        for (Product p : jdbc.query(SELECT + " WHERE p.productId IN (" + marks + ")", this::map, productIds.toArray())) {
            byId.put(p.getProductId(), p);
        }
        List<Product> ordered = new ArrayList<>();
        for (Long id : productIds) {
            if (byId.containsKey(id)) {
                ordered.add(byId.get(id));
            }
        }
        return ordered;
    }

    public List<Product> findSearchable() {
        return jdbc.query(SELECT + " WHERE" + SEARCHABLE + "ORDER BY p.productId", this::map);
    }

    /** Keyword search, the fallback when ai-search is unavailable: every word must appear (NFR06). */
    public List<Product> searchKeyword(String text, String category, int limit) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE" + SEARCHABLE);
        for (String word : text.toLowerCase(Locale.ROOT).split("\\s+")) {
            if (!word.isBlank()) {
                where.append(" AND").append(SEARCH_TEXT);
                args.add("%" + word + "%");
            }
        }
        if (category != null && !category.isBlank()) {
            where.append(" AND p.category = ?");
            args.add(category);
        }
        args.add(limit);
        return jdbc.query(SELECT + where + " ORDER BY p.name LIMIT ?", this::map, args.toArray());
    }

    /** Saves what checkout changes on a product: its available and committed stock. */
    public void save(Product product) {
        int rows = jdbc.update("UPDATE products SET availableStock = ?, committedStock = ? WHERE productId = ?",
                product.getAvailableStock(), product.getCommittedStock(), product.getProductId());
        if (rows != 1) {
            throw new IllegalStateException("Product " + product.getProductId() + " was not saved");
        }
    }

    private Product map(ResultSet r, int row) throws SQLException {
        Seller seller = new Seller(r.getLong("sellerId"), r.getString("sellerName"), r.getString("sellerEmail"),
                UserRole.SELLER);
        Shop shop = new Shop(r.getLong("shopId"), seller, r.getString("shopName"), r.getString("pickupAddress"),
                r.getString("shopStatus"));
        long id = r.getLong("productId");
        String sku = r.getString("sku"), name = r.getString("name"), brand = r.getString("brand");
        String description = r.getString("description"), image = r.getString("imageUrl"), status = r.getString("status");
        long price = r.getLong("price");
        int available = r.getInt("availableStock"), committed = r.getInt("committedStock");
        return switch (r.getString("category")) {
            case "LAPTOP" -> new Laptop(id, shop, sku, name, brand, description, price, available, committed, image,
                    status, r.getString("cpu"), r.getInt("lRam"), r.getInt("lStorage"), r.getDouble("lScreen"),
                    r.getDouble("weightKg"));
            case "PHONE" -> new Phone(id, shop, sku, name, brand, description, price, available, committed, image,
                    status, r.getString("chipset"), r.getInt("pRam"), r.getInt("pStorage"), r.getDouble("pScreen"),
                    r.getInt("batteryMah"));
            default -> new BluetoothHeadphones(id, shop, sku, name, brand, description, price, available, committed,
                    image, status, r.getString("bluetoothVersion"), r.getInt("batteryHours"),
                    r.getBoolean("noiseCancelling"), r.getString("formFactor"));
        };
    }
}
