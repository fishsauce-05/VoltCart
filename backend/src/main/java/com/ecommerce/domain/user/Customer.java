package com.ecommerce.domain.user;

/** A registered buyer. The default address only prefills checkout; each order keeps its own copy (BR07). */
public class Customer {
    private long customerId;
    private String fullName;
    private String email;
    private String phone;
    private String defaultAddress;
    private UserRole role;

    public Customer(long customerId, String fullName, String email, String phone, String defaultAddress, UserRole role) {
        this.customerId = customerId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.defaultAddress = defaultAddress;
        this.role = role;
    }

    /** Only an account with the CUSTOMER role may own a cart and place orders. */
    public boolean canBuy() {
        return role == UserRole.CUSTOMER;
    }

    public long getCustomerId() { return customerId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getDefaultAddress() { return defaultAddress; }
    public UserRole getRole() { return role; }
}
