package com.ecommerce;

import com.ecommerce.application.SearchService;
import com.ecommerce.infrastructure.ProductVectorRepository;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/** Starts the backend. Spring creates the classes of Figure 4.5 and passes each its «use» dependencies. */
@SpringBootApplication
public class EcommerceApplication {
    private static final Logger log = LoggerFactory.getLogger(EcommerceApplication.class);

    public static void main(String[] args) {
        new File(System.getProperty("app.db-path", "data/app.db")).getAbsoluteFile().getParentFile().mkdirs();
        SpringApplication.run(EcommerceApplication.class, args);
    }

    /** Builds the product vectors in the background when none exist yet; search uses keywords until then. */
    @Bean
    ApplicationRunner buildSearchIndex(SearchService searchService, ProductVectorRepository vectors,
                                       @Value("${app.search.build-index-on-start}") boolean enabled) {
        return args -> {
            if (!enabled || vectors.count() > 0) {
                return;
            }
            Thread worker = new Thread(() -> {
                try {
                    searchService.buildIndex();
                } catch (RuntimeException e) {
                    log.warn("Search index not built (is ai-search running?): {}", e.getMessage());
                }
            }, "search-index");
            worker.setDaemon(true);
            worker.start();
        };
    }
}
