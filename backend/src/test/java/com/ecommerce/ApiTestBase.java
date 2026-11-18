package com.ecommerce;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Every test calls the real HTTP endpoints through MockMvc, on a database rebuilt from schema.sql and seed.sql
 * before each test. A small HTTP server stands in for ai-search: its vectors have three dimensions
 * (laptop, phone, headphones) so the expected ranking is known in advance.
 */
@SpringBootTest(properties = "app.search.build-index-on-start=false")
@AutoConfigureMockMvc
public abstract class ApiTestBase {
    static final HttpServer FAKE_AI;
    static volatile boolean aiDown = false;

    static {
        try {
            FAKE_AI = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            FAKE_AI.createContext("/embed/text", ex -> answer(ex, textVector(body(ex))));
            FAKE_AI.createContext("/search/voice", ex -> answer(ex,
                    "\"transcript\":\"noise cancelling headphones\",\"vector\":[0,0,1]"));
            FAKE_AI.createContext("/embed/image", ex -> answer(ex, imageVector(ex.getRequestBody().readAllBytes())));
            FAKE_AI.start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected JdbcTemplate jdbc;
    @Autowired
    private DataSource dataSource;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws IOException {
        Path db = Files.createTempFile("voltcart-test", ".db");
        registry.add("app.db-path", db::toString);
        registry.add("app.ai-search.base-url", () -> "http://127.0.0.1:" + FAKE_AI.getAddress().getPort());
    }

    @BeforeEach
    void freshDatabase() {
        aiDown = false;
        new ResourceDatabasePopulator(new ClassPathResource("db/drop.sql"), new ClassPathResource("db/schema.sql"),
                new ClassPathResource("db/seed.sql")).execute(dataSource);
    }

    @AfterAll
    static void reset() {
        aiDown = false;
    }

    private static String body(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    /** One dimension per category word, as a crude stand-in for a sentence model. */
    private static String textVector(String json) {
        String t = json.toLowerCase(Locale.ROOT);
        int laptop = t.matches("(?s).*\\b(laptop|notebook|thinkpad)\\b.*") ? 1 : 0;
        int phone = t.matches("(?s).*\\b(smartphone|phone|iphone)\\b.*") ? 1 : 0;
        int audio = t.matches("(?s).*\\b(headphones|earbuds)\\b.*") ? 1 : 0;
        return "\"vector\":[" + laptop + "," + phone + "," + audio + "],\"modelVersion\":\"fake\"";
    }

    /** The SHA-256 of the uploaded file as 16 numbers: the same picture gives the same vector, others unrelated ones. */
    private static String imageVector(byte[] multipart) {
        String raw = new String(multipart, StandardCharsets.ISO_8859_1);
        int start = raw.indexOf("\r\n\r\n") + 4, end = raw.lastIndexOf("\r\n--");
        byte[] file = raw.substring(start, Math.max(start, end)).getBytes(StandardCharsets.ISO_8859_1);
        byte[] hash;
        try {
            hash = java.security.MessageDigest.getInstance("SHA-256").digest(file);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        StringBuilder s = new StringBuilder("\"vector\":[");
        for (int i = 0; i < 16; i++) {
            s.append(i == 0 ? "" : ",").append(hash[i]);
        }
        return s.append("],\"modelVersion\":\"fake\"").toString();
    }

    private static void answer(HttpExchange ex, String fields) throws IOException {
        byte[] out = aiDown ? "{\"error\":\"down\"}".getBytes() : ("{" + fields + "}").getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(aiDown ? 503 : 200, out.length);
        ex.getResponseBody().write(out);
        ex.close();
    }
}
