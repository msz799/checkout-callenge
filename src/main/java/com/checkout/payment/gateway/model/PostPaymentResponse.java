package com.checkout.payment.gateway.model;

import java.util.UUID;

/**
 * Response object for the API post payment request. All fields are required
 * @param id UUID of the payment
 * @param status Authorized or Declined
 * @param cardNumberLastFour the last four digits of the card number
 * @param expiryMonth the month the card expires
 * @param expiryYear the year the card expires
 * @param currency the three-letter currency code, one of USD, EUR or GBP
 * @param amount the amount charged in the minor currency unit, e.g. 500 is £5.00 when the currency is GBP
 */
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
