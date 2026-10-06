package com.checkout.payment.gateway.exception;

import java.util.UUID;

public class BankUnavailableException extends RuntimeException {

  private final UUID paymentId;

  public BankUnavailableException(String message, UUID paymentId) {
    this(message, paymentId, null);
  }

  public BankUnavailableException(String message, UUID paymentId, Throwable cause) {
    super(message, cause);
    this.paymentId = paymentId;
  }

  public UUID getPaymentId() {
    return paymentId;
  }
}
