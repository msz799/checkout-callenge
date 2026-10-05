package com.checkout.payment.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PostPaymentBankRequest(
    @JsonProperty("card_number")
    String cardNumber,
    @JsonProperty("expiry_date")
    String expiryDate,
    String currency,
    Integer amount,
    String cvv) {
}
