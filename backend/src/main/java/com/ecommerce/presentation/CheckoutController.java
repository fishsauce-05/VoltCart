package com.ecommerce.presentation;

import com.ecommerce.application.CheckoutService;
import com.ecommerce.domain.order.Order;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HTTP entry of UC10 Checkout and of the order confirmation page. */
@RestController
@RequestMapping("/api")
public class CheckoutController {
    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/checkout")
    public Order checkout(@RequestBody Map<String, Object> request) {
        return checkoutService.checkout(
                CartController.number(request, "customerId"),
                CartController.number(request, "shopId"),
                text(request, "deliveryAddress", 255),
                text(request, "paymentMethod", 20),
                CartController.number(request, "expectedTotal"),
                text(request, "requestReference", 64));
    }

    @GetMapping("/orders/{orderId}")
    public Order getOrder(@PathVariable long orderId) {
        return checkoutService.getOrder(orderId);
    }

    private static String text(Map<String, Object> request, String field, int max) {
        Object value = request == null ? null : request.get(field);
        String s = value == null ? "" : value.toString().trim();
        if (s.length() > max) {
            throw new IllegalArgumentException(field + " is limited to " + max + " characters");
        }
        return s;
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class, NoSuchElementException.class})
    ResponseEntity<Map<String, String>> handleError(RuntimeException e) {
        HttpStatus status = e instanceof NoSuchElementException ? HttpStatus.NOT_FOUND
                : e instanceof IllegalStateException ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
    }
}
