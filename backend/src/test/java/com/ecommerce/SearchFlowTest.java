package com.ecommerce;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;

import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;

/** UC07 Search Products and UC08 View Product Details. */
class SearchFlowTest extends ApiTestBase {

    @BeforeEach
    void index() throws Exception {
        mvc.perform(post("/api/search/index")).andExpect(status().isOk()).andExpect(jsonPath("$.indexed", is(32)));
    }

    @Test
    void textSearchRanksProductsByMeaningAndShowsOnlyProductsOnSale() throws Exception {
        mvc.perform(get("/api/search").param("q", "headphones for travel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(12)))
                .andExpect(jsonPath("$[*].category", everyItem(is("HEADPHONES"))))
                .andExpect(jsonPath("$[*].shop.status", everyItem(is("ACTIVE"))));
    }

    @Test
    void textSearchFallsBackToKeywordsWhenAiSearchIsDown() throws Exception {
        aiDown = true;
        mvc.perform(get("/api/search").param("q", "ThinkPad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].name", containsString("ThinkPad")));
    }

    @Test
    void categoryFilterWithoutTextListsThatCategory() throws Exception {
        mvc.perform(get("/api/search").param("category", "PHONE").param("limit", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(10)))
                .andExpect(jsonPath("$[*].category", everyItem(is("PHONE"))))
                .andExpect(jsonPath("$[*].name", not(hasItem("Nokia G42"))));
    }

    @Test
    void voiceSearchReturnsTheEditableTranscriptAndProducts() throws Exception {
        MockMultipartFile audio = new MockMultipartFile("audio", "query.webm", "audio/webm", new byte[]{1, 2, 3});
        mvc.perform(multipart("/api/search/voice").file(audio))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transcript", is("noise cancelling headphones")))
                .andExpect(jsonPath("$.products[*].category", everyItem(is("HEADPHONES"))));
    }

    @Test
    void imageSearchFindsTheProductOfTheSamePicture() throws Exception {
        byte[] picture;
        try (InputStream in = new ClassPathResource("static/images/p124.png").getInputStream()) {
            picture = in.readAllBytes();
        }
        mvc.perform(multipart("/api/search/image").file(new MockMultipartFile("image", "p124.png", "image/png", picture)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId", is(124)))
                .andExpect(jsonPath("$[0].name", is("Sony WH-1000XM5")));
    }

    @Test
    void imageSearchSaysSoWhenAiSearchIsDown() throws Exception {
        aiDown = true;
        mvc.perform(multipart("/api/search/image").file(new MockMultipartFile("image", "q.png", "image/png", new byte[]{9})))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error", containsString("search by text")));
    }

    @Test
    void uploadThatIsNotAPictureIsRefused() throws Exception {
        mvc.perform(multipart("/api/search/image").file(new MockMultipartFile("image", "a.txt", "text/plain", new byte[]{1})))
                .andExpect(status().isBadRequest());
    }

    @Test
    void productPageShowsSpecificationsOfTheCategory() throws Exception {
        mvc.perform(get("/api/products/103"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("ASUS ROG Zephyrus G14")))
                .andExpect(jsonPath("$.ramGb", is(32)))
                .andExpect(jsonPath("$.specSummary", containsString("RTX 4060")));
    }

    @Test
    void productOfASuspendedShopOrADraftIsNotShown() throws Exception {
        mvc.perform(get("/api/products/112")).andExpect(status().isNotFound());
        mvc.perform(get("/api/products/108")).andExpect(status().isNotFound());
    }
}
