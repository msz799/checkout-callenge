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
    return String.format("%d/%d", expiryMonth, expiryYear);
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
}
