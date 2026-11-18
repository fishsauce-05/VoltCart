package com.ecommerce.domain.product;

import com.ecommerce.common.enums.SearchType;

/**
 * The vectors ai-search computed for one product: one from its text, one from its picture.
 * A text or voice query is compared with the text vector, an image query with the image vector.
 */
public class ProductVector {
    private long productId;
    private float[] textVector;
    private float[] imageVector;
    private String modelVersion;

    public ProductVector(long productId, float[] textVector, float[] imageVector, String modelVersion) {
        this.productId = productId;
        this.textVector = textVector;
        this.imageVector = imageVector;
        this.modelVersion = modelVersion;
    }

    /** Cosine similarity between the query and the vector of the same kind; 0 when that vector is missing. */
    public double similarity(float[] query, SearchType type) {
        float[] own = type == SearchType.IMAGE ? imageVector : textVector;
        if (own == null || query == null || own.length != query.length) {
            return 0;
        }
        double dot = 0, a = 0, b = 0;
        for (int i = 0; i < own.length; i++) {
            dot += own[i] * query[i];
            a += own[i] * own[i];
            b += query[i] * query[i];
        }
        return a == 0 || b == 0 ? 0 : dot / Math.sqrt(a * b);
    }

    public long getProductId() { return productId; }
    public float[] getTextVector() { return textVector; }
    public float[] getImageVector() { return imageVector; }
    public String getModelVersion() { return modelVersion; }
}
