package com.ecommerce.infrastructure;

import com.ecommerce.common.infrastructure.BaseRepository;
import com.ecommerce.domain.product.ProductVector;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Stores each product's vectors as BLOBs of little-endian 32-bit floats. */
@Repository
public class ProductVectorRepository extends BaseRepository {

    public ProductVectorRepository(JdbcTemplate jdbc) {
        super(jdbc);
    }

    public List<ProductVector> findAll() {
        return jdbc.query("SELECT productId, textVector, imageVector, modelVersion FROM productVectors",
                (r, i) -> new ProductVector(r.getLong("productId"), toFloats(r.getBytes("textVector")),
                        toFloats(r.getBytes("imageVector")), r.getString("modelVersion")));
    }

    public void save(ProductVector vector) {
        jdbc.update("INSERT OR REPLACE INTO productVectors (productId, textVector, imageVector, modelVersion) "
                        + "VALUES (?, ?, ?, ?)", vector.getProductId(), toBytes(vector.getTextVector()),
                toBytes(vector.getImageVector()), vector.getModelVersion());
    }

    public int count() {
        Integer n = jdbc.queryForObject("SELECT count(*) FROM productVectors", Integer.class);
        return n == null ? 0 : n;
    }

    private static byte[] toBytes(float[] v) {
        if (v == null) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.allocate(v.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float f : v) {
            buffer.putFloat(f);
        }
        return buffer.array();
    }

    private static float[] toFloats(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        float[] v = new float[bytes.length / 4];
        for (int i = 0; i < v.length; i++) {
            v[i] = buffer.getFloat();
        }
        return v;
    }
}
