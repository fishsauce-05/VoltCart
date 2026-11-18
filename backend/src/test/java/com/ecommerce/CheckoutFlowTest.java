package com.ecommerce;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** UC10 Checkout, including its alternative flows. */
class CheckoutFlowTest extends ApiTestBase {
    static final String ADDRESS = "12 Trần Duy Hưng, Cầu Giấy, Hà Nội";

    void add(long productId, int quantity) throws Exception {
        mvc.perform(post("/api/customers/1/cart/items").contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":" + productId + ",\"quantity\":" + quantity + "}")).andExpect(status().isOk());
    }

    ResultActions checkout(long shopId, long expectedTotal, String method, String reference, String address)
            throws Exception {
        return mvc.perform(post("/api/checkout").contentType(MediaType.APPLICATION_JSON).content(String.format(
                "{\"customerId\":1,\"shopId\":%d,\"deliveryAddress\":\"%s\",\"paymentMethod\":\"%s\","
                        + "\"expectedTotal\":%d,\"requestReference\":\"%s\"}",
                shopId, address, method, expectedTotal, reference)));
    }

    int stock(long productId) {
        return jdbc.queryForObject("SELECT availableStock FROM products WHERE productId = ?", Integer.class, productId);
    }

    int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    @Test
    void checkoutCommitsStockCopiesPricesAndKeepsTheOtherShopsItems() throws Exception {
        add(124, 2);           // SoundWave Audio, 8,490,000 each
        add(101, 1);           // Laptop Hub
        checkout(3, 16980000, "COD", "req-1", ADDRESS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PLACED")))
                .andExpect(jsonPath("$.totalAmount", is(16980000)))
                .andExpect(jsonPath("$.items[0].unitPrice", is(8490000)))
                .andExpect(jsonPath("$.items[0].productName", is("Sony WH-1000XM5")))
                .andExpect(jsonPath("$.payment.status", is("PENDING_COLLECTION")))
                .andExpect(jsonPath("$.deliveryAddress", is(ADDRESS)));
        assertEquals(1, stock(124));
        assertEquals(2, jdbc.queryForObject("SELECT committedStock FROM products WHERE productId = 124", Integer.class));
        mvc.perform(get("/api/customers/1/cart"))
                .andExpect(jsonPath("$.items.length()", is(1)))
                .andExpect(jsonPath("$.items[0].product.productId", is(101)));
    }

    @Test
    void theOrderKeepsItsPriceWhenTheProductPriceChangesLater() throws Exception {
        add(127, 1);
        String body = checkout(3, 1190000, "COD", "req-snap", ADDRESS).andReturn().getResponse().getContentAsString();
        long orderId = Long.parseLong(body.replaceAll("(?s).*\"orderId\":(\\d+).*", "$1"));
        jdbc.update("UPDATE products SET price = 990000 WHERE productId = 127");
        mvc.perform(get("/api/orders/" + orderId)).andExpect(jsonPath("$.items[0].unitPrice", is(1190000)));
    }

    @Test
    void aRepeatedRequestReturnsTheSameOrderAndCreatesNoSecondOne() throws Exception {
        add(125, 1);
        String first = checkout(3, 5990000, "COD", "req-twice", ADDRESS).andReturn().getResponse().getContentAsString();
        String second = checkout(3, 5990000, "COD", "req-twice", ADDRESS).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals(first, second);
        assertEquals(1, count("orders"));
        assertEquals(11, stock(125));
    }

    @Test
    void aChangedPriceCreatesNoOrderAndShowsTheNewTotal() throws Exception {
        add(126, 1);
        jdbc.update("UPDATE products SET price = 9490000 WHERE productId = 126");
        checkout(3, 9990000, "COD", "req-price", ADDRESS)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("9,490,000 VND")));
        assertEquals(0, count("orders"));
        assertEquals(4, stock(126));
    }

    @Test
    void notEnoughStockRollsBackTheWholeOrder() throws Exception {
        add(101, 1);           // Dell XPS 13, stock 6
        add(103, 2);           // ROG Zephyrus G14, stock 2
        jdbc.update("UPDATE products SET availableStock = 1 WHERE productId = 103");   // another buyer took one
        checkout(1, 39990000 + 2 * 49990000L, "BANK_TRANSFER", "req-stock", ADDRESS)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("Only 1 left of ASUS ROG Zephyrus G14")));
        assertEquals(6, stock(101));
        assertEquals(0, count("orders"));
        assertEquals(2, count("cartItems"));
    }

    @Test
    void cashOnDeliveryAboveFiftyMillionIsRefusedButBankTransferIsAccepted() throws Exception {
        add(109, 1);           // MacBook Pro 14 M4 Pro, 52,990,000
        checkout(4, 52990000, "COD", "req-cod", ADDRESS)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("bank transfer")));
        checkout(4, 52990000, "BANK_TRANSFER", "req-bank", ADDRESS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("AWAITING_PAYMENT")))
                .andExpect(jsonPath("$.payment.status", is("AWAITING_TRANSFER")));
    }

    @Test
    void anEmptyAddressIsRefused() throws Exception {
        add(130, 1);
        checkout(3, 790000, "COD", "req-addr", " ").andExpect(status().isBadRequest());
        assertEquals(0, count("orders"));
    }

    @Test
    void theDatabaseItselfRefusesNegativeStock() {
        DataAccessException e =
            assertThrows(DataAccessException.class,
                () -> jdbc.update("UPDATE products SET availableStock = -1 WHERE productId = 101"));
        assertTrue(e.getMessage().contains("CHECK constraint failed: availableStock >= 0"));
    }
}
