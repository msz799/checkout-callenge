package com.checkout.payment.gateway.exception;

import java.util.UUID;

public class PaymentNotFoundException extends RuntimeException{
  private final UUID paymentId;
  public PaymentNotFoundException(UUID id) {
    super();
    this.paymentId = id;
  }

  public UUID getPaymentId() {
    return paymentId;
  }
}
