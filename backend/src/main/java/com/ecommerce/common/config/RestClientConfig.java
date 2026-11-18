package com.ecommerce.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** The HTTP client SearchService uses to reach the Python ai-search service. */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient aiSearchClient(@Value("${app.ai-search.base-url}") String baseUrl,
                                     @Value("${app.ai-search.timeout-ms}") int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);           // fail fast when ai-search is not running (NFR06)
        factory.setReadTimeout(timeoutMs);         // the first call loads the models and can take long
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }
}
