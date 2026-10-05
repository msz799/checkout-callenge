package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.model.ErrorResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("api")
@Tag(name = "Payments", description = "Payment Gateway")
public class PaymentGatewayController {

  private static final String POST_PAYMENT_EXAMPLE = """
      {
        "card_number": "69670223445168",
        "expiry_month": 10,
        "expiry_year": 2026,
        "currency": "GBP",
        "amount": 500,
        "cvv": "440"
      }
      """;

  private static final String POST_PAYMENT_DESCRIPTION = """
      All fields are required.
      
      - **card_number**: 14 to 19 digits
      - **expiry_month**: a value between 1 and 12
      - **expiry_year**: together with expiry_month it must be the current month or later
      - **currency**: the three-letter currency code, one of USD, EUR or GBP
      - **amount**: the amount to charge in the minor currency unit, e.g. 500 is £5.00 when the currency is GBP
      - **cvv**: the 3 or 4 digit security code on the back of the card
      """;

  private static final String PAYMENT_RESPONSE_DESCRIPTION = """
      - **id**: UUID of the payment
      - **status**: Authorized or Declined
      - **cardNumberLastFour**: the last four digits of the card number
      - **expiryMonth**: the month the card expires
      - **expiryYear**: the year the card expires
      - **currency**: the three-letter currency code, one of USD, EUR or GBP
      - **amount**: the amount charged in the minor currency unit, e.g. 500 is £5.00 when the currency is GBP
      """;

  private static final String PAYMENT_RESPONSE_EXAMPLE = """
      {
        "id": "643ff75a-3084-467c-9ecf-f2e112ea6f50",
        "status": "Declined",
        "cardNumberLastFour": "5168",
        "expiryMonth": 10,
        "expiryYear": 2026,
        "currency": "GBP",
        "amount": 500
      }
      """;

  private static final String PAYMENT_NOT_FOUND_EXAMPLE = """
      {
        "message": "Payment not found"
      }
      """;

  private static final String VALIDATION_ERROR_EXAMPLE = """
      {
        "message": "Rejected: card_number must be 14-19 numeric characters long"
      }
      """;

  private static final String BANK_UNAVAILABLE_EXAMPLE = """
      {
        "message": "Acquiring bank is unavailable"
      }
      """;

  private final PaymentGatewayService paymentGatewayService;

  public PaymentGatewayController(PaymentGatewayService paymentGatewayService) {
    this.paymentGatewayService = paymentGatewayService;
  }

  @GetMapping("/payment/{id}")
  @Operation(summary = "Retrieve a payment", description = "Returns the details of a previously processed payment")
  @ApiResponse(
      responseCode = "200",
      description = "Payment found\n\n" + PAYMENT_RESPONSE_DESCRIPTION,
      content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = PostPaymentResponse.class),
          examples = @ExampleObject(value = PAYMENT_RESPONSE_EXAMPLE)))
  @ApiResponse(
      responseCode = "404",
      description = "No payment exists with the given ID",
      content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ErrorResponse.class),
          examples = @ExampleObject(value = PAYMENT_NOT_FOUND_EXAMPLE)))
  public ResponseEntity<PostPaymentResponse> getPayment(
      @Parameter(
          description = "UUID of the payment",
          example = "643ff75a-3084-467c-9ecf-f2e112ea6f50")
      @PathVariable UUID id) {
    return new ResponseEntity<>(paymentGatewayService.getPayment(id), HttpStatus.OK);
  }

  @PostMapping("/payment")
  @Operation(summary = "Process a payment", description = "Sends the payment to the acquiring bank")
  @ApiResponse(
      responseCode = "200",
      description = "Payment processed (payment status is Authorized or Declined)\n\n" + PAYMENT_RESPONSE_DESCRIPTION,
      content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = PostPaymentResponse.class),
          examples = @ExampleObject(value = PAYMENT_RESPONSE_EXAMPLE)))
  @ApiResponse(
      responseCode = "422",
      description = "Request failed validation (payment status is Rejected)",
      content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ErrorResponse.class),
          examples = @ExampleObject(value = VALIDATION_ERROR_EXAMPLE)))
  @ApiResponse(
      responseCode = "503",
      description = "Acquiring bank is unavailable",
      content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ErrorResponse.class),
          examples = @ExampleObject(value = BANK_UNAVAILABLE_EXAMPLE)))
  public ResponseEntity<PostPaymentResponse> postPayment(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = POST_PAYMENT_DESCRIPTION,
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(value = POST_PAYMENT_EXAMPLE)))
      @Valid @RequestBody PostPaymentRequest request) {
    return new ResponseEntity<>(paymentGatewayService.processPayment(request), HttpStatus.OK);
  }
}
