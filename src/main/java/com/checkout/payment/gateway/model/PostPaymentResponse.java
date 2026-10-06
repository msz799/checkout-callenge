package com.checkout.payment.gateway.model;

import java.util.UUID;

public record PostPaymentResponse(
    UUID id,
    String status,
    String cardNumberLastFour,
    int expiryMonth,
    int expiryYear,
    String currency,
    int amount
) {
}
