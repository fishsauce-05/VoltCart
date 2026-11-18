package com.ecommerce.application;

import com.ecommerce.common.enums.SearchType;
import com.ecommerce.domain.product.Product;
import com.ecommerce.domain.product.ProductVector;
import com.ecommerce.infrastructure.ProductRepository;
import com.ecommerce.infrastructure.ProductVectorRepository;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/**
 * UC07 Search Products. The Python ai-search service turns a query into a vector; this service compares it with
 * the stored product vectors, keeps the products a customer may buy, and falls back to keyword search when
 * ai-search cannot be reached (NFR06).
 */
@Service
public class SearchService {
    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final ProductRepository productRepository;
    private final ProductVectorRepository productVectorRepository;
    private final RestClient aiSearchClient;
    private final double minTextScore;
    private final double minImageScore;
    private final double imageMargin;

    public SearchService(ProductRepository productRepository, ProductVectorRepository productVectorRepository,
                         RestClient aiSearchClient,
                         @Value("${app.search.min-text-score}") double minTextScore,
                         @Value("${app.search.min-image-score}") double minImageScore,
                         @Value("${app.search.image-margin}") double imageMargin) {
        this.productRepository = productRepository;
        this.productVectorRepository = productVectorRepository;
        this.aiSearchClient = aiSearchClient;
        this.minTextScore = minTextScore;
        this.minImageScore = minImageScore;
        this.imageMargin = imageMargin;
    }

    /** Semantic search by text; an empty text lists the catalogue, an unreachable ai-search gives keyword search. */
    public List<Product> searchText(String text, String category, int limit) {
        if (text == null || text.isBlank()) {
            return productRepository.findSearchable().stream()
                    .filter(p -> category == null || category.isBlank() || category.equals(p.getCategory()))
                    .limit(limit).toList();
        }
        if (productVectorRepository.count() == 0) {
            return productRepository.searchKeyword(text, category, limit);
        }
        try {
            float[] query = vectorOf(aiSearchClient.post().uri("/embed/text").contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", text)).retrieve().body(Map.class));
            return rank(query, SearchType.TEXT, category, limit, minTextScore);
        } catch (RestClientException e) {
            log.warn("ai-search unavailable, keyword search used: {}", e.getMessage());
            return productRepository.searchKeyword(text, category, limit);
        }
    }

    /** Spoken query: ai-search transcribes and encodes it; the transcript comes back so the customer can edit it. */
    public Map<String, Object> searchVoice(byte[] audio, String filename, int limit) {
        Map<?, ?> answer;
        try {
            answer = aiSearchClient.post().uri("/search/voice").contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(file("audio", audio, filename)).retrieve().body(Map.class);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Voice search is unavailable right now; please type your query", e);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("transcript", answer.get("transcript"));
        result.put("products", rank(vectorOf(answer), SearchType.VOICE, null, limit, minTextScore));
        return result;
    }

    /** Image query: products whose picture is close to the uploaded photo. */
    public List<Product> searchImage(byte[] image, String filename, int limit) {
        try {
            float[] query = vectorOf(aiSearchClient.post().uri("/embed/image").contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(file("image", image, filename)).retrieve().body(Map.class));
            return rank(query, SearchType.IMAGE, null, limit, minImageScore);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Image search is unavailable right now; please search by text", e);
        }
    }

    /** UC08: the product page. A product that is not on sale is not shown (BR01). */
    public Product viewProduct(long productId) {
        Product product = productRepository.findById(productId);
        if (product == null || !product.isSearchable()) {
            throw new NoSuchElementException("Product " + productId + " is not available");
        }
        return product;
    }

    /** Computes and stores the text and image vectors of every searchable product; returns how many. */
    public int buildIndex() {
        int count = 0;
        for (Product product : productRepository.findSearchable()) {
            Map<?, ?> text = aiSearchClient.post().uri("/embed/text").contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", product.indexText())).retrieve().body(Map.class);
            float[] imageVector = null;
            byte[] picture = picture(product.getImageUrl());
            if (picture != null) {
                imageVector = vectorOf(aiSearchClient.post().uri("/embed/image")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(file("image", picture, "p" + product.getProductId() + ".png"))
                        .retrieve().body(Map.class));
            }
            productVectorRepository.save(new ProductVector(product.getProductId(), vectorOf(text), imageVector,
                    String.valueOf(text.get("modelVersion"))));
            count++;
        }
        log.info("Indexed {} products", count);
        return count;
    }

    /**
     * Ranks the stored vectors by similarity and keeps the scores above the threshold, then the products on sale.
     * Image scores of different products lie close together (Chapter 5), so for an image query a product must
     * also be within imageMargin of the best score.
     */
    private List<Product> rank(float[] query, SearchType type, String category, int limit, double minScore) {
        record Scored(long productId, double score) { }
        List<Scored> scored = productVectorRepository.findAll().stream()
                .map(v -> new Scored(v.getProductId(), v.similarity(query, type)))
                .sorted(Comparator.comparingDouble(Scored::score).reversed())
                .toList();
        double best = scored.isEmpty() ? 0 : scored.get(0).score();
        double cut = type == SearchType.IMAGE ? Math.max(minScore, best - imageMargin) : minScore;
        List<Long> ids = new ArrayList<>();
        scored.stream().filter(s -> s.score() >= cut).forEach(s -> ids.add(s.productId()));
        return productRepository.findByIds(ids).stream()
                .filter(Product::isSearchable)
                .filter(p -> category == null || category.isBlank() || category.equals(p.getCategory()))
                .limit(limit).toList();
    }

    private static float[] vectorOf(Map<?, ?> answer) {
        Object raw = answer == null ? null : answer.get("vector");
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            throw new RestClientException("ai-search returned no vector");
        }
        float[] v = new float[list.size()];
        for (int i = 0; i < v.length; i++) {
            v[i] = ((Number) list.get(i)).floatValue();
        }
        return v;
    }

    private static MultiValueMap<String, Object> file(String field, byte[] bytes, String filename) {
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add(field, new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename == null || filename.isBlank() ? field : filename;
            }
        });
        return parts;
    }

    private static byte[] picture(String imageUrl) {
        ClassPathResource resource = new ClassPathResource("static" + imageUrl);
        if (!resource.exists()) {
            return null;
        }
        try (InputStream in = resource.getInputStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }
}
