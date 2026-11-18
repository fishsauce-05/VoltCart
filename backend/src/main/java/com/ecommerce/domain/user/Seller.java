package com.ecommerce.domain.user;

/** The person who operates a Shop. */
public class Seller {
    private long sellerId;
    private String fullName;
    private String email;
    private UserRole role;

    public Seller(long sellerId, String fullName, String email, UserRole role) {
        this.sellerId = sellerId;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
    }

    public long getSellerId() { return sellerId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public UserRole getRole() { return role; }
}
