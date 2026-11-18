package com.ecommerce;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** UC09 Manage Cart. */
class CartFlowTest extends ApiTestBase {

    ResultActions add(long customerId, long productId, int quantity) throws Exception {
        return mvc.perform(post("/api/customers/" + customerId + "/cart/items").contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":" + productId + ",\"quantity\":" + quantity + "}"));
    }

    @Test
    void addingProductsOfTwoShopsKeepsEachWithItsShop() throws Exception {
        add(1, 101, 1).andExpect(status().isOk());
        add(1, 124, 2).andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()", is(2)))
                .andExpect(jsonPath("$.items[0].product.shop.shopName", is("Laptop Hub")))
                .andExpect(jsonPath("$.items[1].product.shop.shopName", is("SoundWave Audio")))
                .andExpect(jsonPath("$.items[1].lineAmount", is(16980000)))
                .andExpect(jsonPath("$.customer.defaultAddress", containsString("Trần Duy Hưng")));
    }

    @Test
    void addingTheSameProductAgainIncreasesItsQuantity() throws Exception {
        add(1, 113, 1);
        add(1, 113, 2).andExpect(jsonPath("$.items.length()", is(1))).andExpect(jsonPath("$.items[0].quantity", is(3)));
    }

    @Test
    void quantityAboveTheStockIsRefusedAndNothingIsSaved() throws Exception {
        add(1, 103, 3).andExpect(status().isConflict()).andExpect(jsonPath("$.error", is("Only 2 left of ASUS ROG Zephyrus G14")));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM cartItems", Integer.class));
    }

    @Test
    void moreThanFiveUnitsOfOneProductAreRefused() throws Exception {
        add(1, 118, 4).andExpect(status().isOk());
        add(1, 118, 2).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error", containsString("At most 5")));
    }

    @Test
    void addingReservesNoStock() throws Exception {
        add(1, 124, 2).andExpect(status().isOk());
        assertEquals(3, jdbc.queryForObject("SELECT availableStock FROM products WHERE productId = 124", Integer.class));
    }

    @Test
    void quantityCanBeChangedAndAnItemRemoved() throws Exception {
        add(1, 101, 1);
        add(1, 125, 1);
        mvc.perform(patch("/api/customers/1/cart/items/125").contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":3}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[1].quantity", is(3)));
        mvc.perform(patch("/api/customers/1/cart/items/125").contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":0}"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/customers/1/cart/items/101"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()", is(1)));
        mvc.perform(get("/api/customers/1/cart")).andExpect(jsonPath("$.items[0].product.productId", is(125)));
    }

    @Test
    void productThatIsNotOnSaleCannotBeAdded() throws Exception {
        add(1, 112, 1).andExpect(status().isConflict()).andExpect(jsonPath("$.error", containsString("not on sale")));
    }

    @Test
    void anAdministratorAccountCannotShop() throws Exception {
        mvc.perform(get("/api/customers/3/cart")).andExpect(status().isConflict());
        mvc.perform(get("/api/customers/99/cart")).andExpect(status().isNotFound());
    }
}
