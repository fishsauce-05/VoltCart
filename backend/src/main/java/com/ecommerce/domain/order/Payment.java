package com.ecommerce.domain.order;

/** How the customer pays for one order, and how far that payment has got. */
public class Payment {
    public static final String COD = "COD";
    public static final String BANK_TRANSFER = "BANK_TRANSFER";

    private long paymentId;
    private String method;
    private long amount;
    private String status;
    private String reference;

    public Payment(long paymentId, String method, long amount, String status, String reference) {
        this.paymentId = paymentId;
        this.method = method;
        this.amount = amount;
        this.status = status;
        this.reference = reference;
    }

    public boolean isCashOnDelivery() {
        return COD.equals(method);
    }

    public long getPaymentId() { return paymentId; }
    public String getMethod() { return method; }
    public long getAmount() { return amount; }
    public String getStatus() { return status; }
    public String getReference() { return reference; }
}
