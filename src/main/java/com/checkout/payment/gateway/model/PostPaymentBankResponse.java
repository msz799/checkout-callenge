package com.checkout.payment.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Processing payment response object from the acquiring bank
 */
public record PostPaymentBankResponse(
    boolean authorized,
    @JsonProperty("authorization_code")
    String authorizationCode) {
}
