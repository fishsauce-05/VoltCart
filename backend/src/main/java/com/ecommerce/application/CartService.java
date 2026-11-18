package com.ecommerce.application;

import com.ecommerce.domain.order.Cart;
import com.ecommerce.domain.product.Product;
import com.ecommerce.infrastructure.CartRepository;
import com.ecommerce.infrastructure.ProductRepository;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** UC09 Manage Cart. The rules live in Cart; this service loads, calls and saves. */
@Service
public class CartService {
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public CartService(CartRepository cartRepository, ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }

    /** The customer's active cart; a new empty one when the last was checked out. */
    @Transactional
    public Cart getCart(long customerId) {
        Cart cart = cartRepository.findActiveByCustomer(customerId);
        return cart != null ? cart : cartRepository.create(customerId);
    }

    @Transactional
    public Cart addItem(long customerId, long productId, int quantity) {
        Cart cart = getCart(customerId);
        Product product = productRepository.findById(productId);
        if (product == null) {
            throw new NoSuchElementException("Product " + productId + " does not exist");
        }
        cart.addItem(product, quantity);
        cartRepository.save(cart);
        return getCart(customerId);
    }

    @Transactional
    public Cart updateQuantity(long customerId, long productId, int quantity) {
        Cart cart = getCart(customerId);
        cart.updateQuantity(productId, quantity);
        cartRepository.save(cart);
        return getCart(customerId);
    }

    @Transactional
    public Cart removeItem(long customerId, long productId) {
        Cart cart = getCart(customerId);
        cart.removeItem(productId);
        cartRepository.save(cart);
        return getCart(customerId);
    }
}
