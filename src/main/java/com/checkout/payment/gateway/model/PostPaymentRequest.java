package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.enums.Currency;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.io.Serializable;
import java.time.DateTimeException;
import java.time.YearMonth;
import java.util.Arrays;

/**
 * Request object for the API post payment request. All fields are required
 *
 * @param cardNumber  must be 14-19 numeric characters long
 * @param expiryMonth the month the card expires, must be a digit between 1 and 12
 * @param expiryYear  the year the card expires, expiry_month/expiry_year must be in the future
 * @param currency    the three-letter currency code, one of USD, EUR or GBP
 * @param amount      the amount charged in the minor currency unit, e.g. 500 is £5.00 when the currency is GBP
 * @param cvv         the 3 or 4 digit security code on the back of the card
 */
public record PostPaymentRequest(
    @NotBlank(message = "card_number is required")
    @Pattern(regexp = "[0-9]{14,19}", message = "card_number must be 14-19 numeric characters long")
    @JsonProperty("card_number")
    String cardNumber,
    @JsonProperty("expiry_month")
    int expiryMonth,
    @JsonProperty("expiry_year")
    int expiryYear,
    String currency,
    @NotNull(message = "amount is required")
    @Positive(message = "amount must be an integer greater than zero")
    Integer amount,
    @NotBlank(message = "cvv is required")
    @Pattern(regexp = "[0-9]{3,4}", message = "cvv must be 3-4 numeric characters long")
    String cvv
) implements Serializable {

  @JsonIgnore()
  public String getExpiryDate() {
    return String.format("%02d/%d", expiryMonth, expiryYear);
  }

  @JsonIgnore
  @AssertTrue(message = "expiry_month/expiry_year are required and must be in the future")
  public boolean isExpiryDateValid() {
    try {
      return !YearMonth.of(expiryYear, expiryMonth).isBefore(YearMonth.now());
    } catch (DateTimeException ex) {
      return false;
    }
  }

  @JsonIgnore
  @AssertTrue(message = "currency is required and must be one of USD, EUR, GBP")
  public boolean isCurrencyValid() {
    return currency != null && Arrays.stream(Currency.values())
        .anyMatch(c -> c.getCode().equals(currency));
  }

  @Override
  public String toString() {
    var maskedCardNumber = cardNumber == null || cardNumber.length() < 4
        ? "***" : "***" + cardNumber.substring(cardNumber.length() - 4);
    return "PostPaymentRequest[cardNumber=%s, expiryMonth=%d, expiryYear=%d, currency=%s, amount=%s, cvv=***]"
        .formatted(maskedCardNumber, expiryMonth, expiryYear, currency, amount);
  }
}
