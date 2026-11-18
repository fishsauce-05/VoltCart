package com.ecommerce.presentation;

import com.ecommerce.application.CartService;
import com.ecommerce.domain.order.Cart;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HTTP entry of UC09 Manage Cart. */
@RestController
@RequestMapping("/api/customers/{customerId}/cart")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public Cart getCart(@PathVariable long customerId) {
        return cartService.getCart(customerId);
    }

    @PostMapping("/items")
    public Cart addItem(@PathVariable long customerId, @RequestBody Map<String, Object> request) {
        return cartService.addItem(customerId, number(request, "productId"), (int) number(request, "quantity"));
    }

    @PatchMapping("/items/{productId}")
    public Cart updateQuantity(@PathVariable long customerId, @PathVariable long productId,
                               @RequestBody Map<String, Object> request) {
        return cartService.updateQuantity(customerId, productId, (int) number(request, "quantity"));
    }

    @DeleteMapping("/items/{productId}")
    public Cart removeItem(@PathVariable long customerId, @PathVariable long productId) {
        return cartService.removeItem(customerId, productId);
    }

    /** Reads a required whole number from the JSON body, or explains what is wrong. */
    static long number(Map<String, Object> request, String field) {
        Object value = request == null ? null : request.get(field);
        if (value instanceof Number n && n.doubleValue() == Math.rint(n.doubleValue())) {
            return n.longValue();
        }
        throw new IllegalArgumentException(field + " must be a whole number");
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class, NoSuchElementException.class})
    ResponseEntity<Map<String, String>> handleError(RuntimeException e) {
        HttpStatus status = e instanceof NoSuchElementException ? HttpStatus.NOT_FOUND
                : e instanceof IllegalStateException ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
    }
}
