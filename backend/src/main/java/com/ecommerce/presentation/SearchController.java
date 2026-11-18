package com.ecommerce.presentation;

import com.ecommerce.application.SearchService;
import com.ecommerce.domain.product.Product;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** HTTP entry of UC07 and UC08. It checks the input and calls SearchService; it holds no business rule. */
@RestController
@RequestMapping("/api")
public class SearchController {
    private static final int MAX_QUERY = 200;

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public List<Product> searchText(@RequestParam(defaultValue = "") String q,
                                    @RequestParam(required = false) String category,
                                    @RequestParam(defaultValue = "12") int limit) {
        if (q.length() > MAX_QUERY) {
            throw new IllegalArgumentException("The query is limited to " + MAX_QUERY + " characters");
        }
        if (category != null && !category.isBlank() && !List.of("LAPTOP", "PHONE", "HEADPHONES").contains(category)) {
            throw new IllegalArgumentException("Unknown category: " + category);
        }
        return searchService.searchText(q.trim(), category, checkLimit(limit));
    }

    @PostMapping("/search/voice")
    public Map<String, Object> searchVoice(@RequestPart("audio") MultipartFile audio,
                                           @RequestParam(defaultValue = "12") int limit) throws IOException {
        if (audio.isEmpty()) {
            throw new IllegalArgumentException("The recording is empty");
        }
        return searchService.searchVoice(audio.getBytes(), audio.getOriginalFilename(), checkLimit(limit));
    }

    @PostMapping("/search/image")
    public List<Product> searchImage(@RequestPart("image") MultipartFile image,
                                     @RequestParam(defaultValue = "12") int limit) throws IOException {
        String type = image.getContentType();
        if (image.isEmpty() || type == null || !type.startsWith("image/")) {
            throw new IllegalArgumentException("Please upload a JPEG or PNG picture");
        }
        return searchService.searchImage(image.getBytes(), image.getOriginalFilename(), checkLimit(limit));
    }

    @GetMapping("/products/{productId}")
    public Product viewProduct(@PathVariable long productId) {
        return searchService.viewProduct(productId);
    }

    /** Recomputes the product vectors, for example after the catalogue changed. */
    @PostMapping("/search/index")
    public Map<String, Object> rebuildIndex() {
        return Map.of("indexed", searchService.buildIndex());
    }

    private static int checkLimit(int limit) {
        if (limit < 1 || limit > 50) {
            throw new IllegalArgumentException("limit must be between 1 and 50");
        }
        return limit;
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class, NoSuchElementException.class,
            ResponseStatusException.class})
    ResponseEntity<Map<String, String>> handleError(RuntimeException e) {
        if (e instanceof ResponseStatusException r) {
            return ResponseEntity.status(r.getStatusCode()).body(Map.of("error", String.valueOf(r.getReason())));
        }
        HttpStatus status = e instanceof NoSuchElementException ? HttpStatus.NOT_FOUND
                : e instanceof IllegalStateException ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
    }
}
