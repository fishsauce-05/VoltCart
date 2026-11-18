package com.ecommerce.application;

import com.ecommerce.domain.order.Cart;
import com.ecommerce.domain.order.CartItem;
import com.ecommerce.domain.order.Order;
import com.ecommerce.domain.product.Product;
import com.ecommerce.infrastructure.CartRepository;
import com.ecommerce.infrastructure.OrderRepository;
import com.ecommerce.infrastructure.ProductRepository;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** UC10 Checkout: one order for the items of one shop, in one transaction. */
@Service
public class CheckoutService {
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public CheckoutService(CartRepository cartRepository, ProductRepository productRepository,
                           OrderRepository orderRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Steps of Figure 2.8: reuse the order of a repeated request (BR10); compare the shop subtotal with the total
     * the customer saw (BR04); commit the stock of each item (BR05); copy prices into the order; create the
     * payment; remove only that shop's items from the cart. Any failure rolls the whole transaction back.
     */
    @Transactional
    public Order checkout(long customerId, long shopId, String deliveryAddress, String paymentMethod,
                          long expectedTotal, String requestReference) {
        if (requestReference == null || requestReference.isBlank()) {
            throw new IllegalArgumentException("A request reference is required");
        }
        Order existing = orderRepository.findByRequestReference(requestReference);
        if (existing != null) {
            if (existing.getCustomer().getCustomerId() != customerId) {
                throw new IllegalArgumentException("This request reference belongs to another order");
            }
            return existing;
        }
        if (deliveryAddress == null || deliveryAddress.isBlank()) {
            throw new IllegalArgumentException("A delivery address is required");
        }
        Cart cart = cartRepository.findActiveByCustomer(customerId);
        List<CartItem> items = cart == null ? List.of() : cart.itemsOfShop(shopId);
        if (items.isEmpty()) {
            throw new IllegalStateException("The cart has no items of this shop");
        }
        long subtotal = cart.shopSubtotal(shopId);
        if (subtotal != expectedTotal) {
            throw new IllegalStateException(String.format(
                    "Prices have changed: the total is now %,d VND instead of %,d VND; please review the cart",
                    subtotal, expectedTotal));
        }
        Order order = new Order(0, cart.getCustomer(), items.get(0).getProduct().getShop(), "AWAITING_PAYMENT",
                deliveryAddress.trim(), 0, requestReference, null, List.of(), null);
        for (CartItem item : items) {
            Product product = productRepository.findById(item.getProduct().getProductId());
            if (product == null || !product.isSearchable()) {
                throw new IllegalStateException(item.getProduct().getName() + " is no longer on sale");
            }
            product.commitStock(item.getQuantity());
            order.addItem(product, item.getQuantity());
            productRepository.save(product);
        }
        order.addPayment(paymentMethod);
        cart.removeShopItems(shopId);
        cartRepository.save(cart);
        return orderRepository.save(order);
    }

    public Order getOrder(long orderId) {
        Order order = orderRepository.findById(orderId);
        if (order == null) {
            throw new NoSuchElementException("Order " + orderId + " does not exist");
        }
        return order;
    }
}
