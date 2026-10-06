package com.checkout.payment.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Processing payment request object at the acquiring bank
 */
public record PostPaymentBankRequest(
    @JsonProperty("card_number")
    String cardNumber,
    @JsonProperty("expiry_date")
    String expiryDate,
    String currency,
    Integer amount,
    String cvv) {

  @Override
  public String toString() {
    var cardNumberLastFour = cardNumber.substring(cardNumber.length() - 4);
    return "PostPaymentBankRequest[cardNumber=%s, expiryDate=%s, currency=%s, amount=%s, cvv=***]"
        .formatted(cardNumberLastFour, expiryDate, currency, amount);
  }
}
