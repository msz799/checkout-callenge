package com.checkout.payment.gateway.controller;


import static java.time.YearMonth.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.PostPaymentBankRequest;
import com.checkout.payment.gateway.model.PostPaymentBankResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import com.checkout.payment.gateway.service.BankCommunicator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentGatewayControllerTest {

  @Autowired
  private MockMvc mvc;
  @Autowired
  ObjectMapper objectMapper;
  @MockBean
  BankCommunicator bankCommunicator;
  private static final int futureYear = now().getYear() + 1;
  private static final String CARD_NUMBER = "111111111111111";
  private static final int EXPIRY_MONTH = 12;
  private static final String CURRENCY = "GBP";
  private static final int AMOUNT = 1;
  private static final String CVV = "123";
  private static final String UUID_PATTERN =
      "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

  private PostPaymentRequest validPostRequest() {
    return new PostPaymentRequest(CARD_NUMBER, EXPIRY_MONTH, futureYear, CURRENCY, AMOUNT, CVV);
  }

  @Test
  void whenPaymentProcessAuthorizedThenGetPaymentReturnsIt() throws Exception {
    var request = validPostRequest();
    var bankRequest = new PostPaymentBankRequest(
        request.cardNumber(),
        request.getExpiryDate(),
        request.currency(),
        request.amount(),
        request.cvv());
    var bankResponse = new ResponseEntity<>(
        new PostPaymentBankResponse(true, "0bb07405-6d44-4b50-a14f-7ae0beff13ad"), HttpStatus.OK);
    when(bankCommunicator.sendPayment(eq(bankRequest), any())).thenReturn(bankResponse);

    var postResponse = mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", matchesPattern(UUID_PATTERN)))
        .andExpect(jsonPath("$.status").value(PaymentStatus.AUTHORIZED.getName()))
        .andExpect(jsonPath("$.cardNumberLastFour").value("1111"))
        .andExpect(jsonPath("$.expiryMonth").value(request.expiryMonth()))
        .andExpect(jsonPath("$.expiryYear").value(request.expiryYear()))
        .andExpect(jsonPath("$.currency").value(request.currency()))
        .andExpect(jsonPath("$.amount").value(request.amount()))
        .andReturn();

    var paymentId = objectMapper.readValue(postResponse.getResponse().getContentAsString(), PostPaymentResponse.class).id();
    mvc.perform(MockMvcRequestBuilders.get("/payment/" + paymentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(PaymentStatus.AUTHORIZED.getName()))
        .andExpect(jsonPath("$.cardNumberLastFour").value("1111"))
        .andExpect(jsonPath("$.expiryMonth").value(request.expiryMonth()))
        .andExpect(jsonPath("$.expiryYear").value(request.expiryYear()))
        .andExpect(jsonPath("$.currency").value(request.currency()))
        .andExpect(jsonPath("$.amount").value(request.amount()));
  }

  @Test
  void whenPaymentWithIdDoesNotExistThen404IsReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/payment/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Payment not found"));
  }

  static Stream<Arguments> processPaymentCardNumberErrorProvider() {
    var characterError = "card_number must be 14-19 numeric characters long";
    var requiredError = "card_number is required";
    // card_number, expected errors
    return Stream.of(
        arguments("3444423", List.of(characterError)),
        arguments("", List.of(characterError, requiredError)),
        arguments(null, List.of(requiredError)),
        arguments("-12345678912345", List.of(characterError)),
        arguments("12345678912345111111", List.of(characterError)));
  }

  @ParameterizedTest
  @MethodSource("processPaymentCardNumberErrorProvider")
  void whenPaymentProcessWithWrongCardNumberThen422IsReturned(String cardNumber,
      List<String> errorMessages) throws Exception {
    var request = new PostPaymentRequest(cardNumber, EXPIRY_MONTH, futureYear, CURRENCY, AMOUNT,
        CVV);
    var result = mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnprocessableEntity());
    for (var errorMessage : errorMessages) {
      result.andExpect(jsonPath("$.message", containsString(errorMessage)));
    }
  }

  static Stream<Arguments> processPaymentExpiryMonthErrorProvider() {
    // expiry_month
    return Stream.of(arguments(0), arguments(13));
  }

  @ParameterizedTest
  @MethodSource("processPaymentExpiryMonthErrorProvider")
  void whenPaymentProcessWithWrongExpiryMonthThen422IsReturned(int expiryMonth) throws Exception {
    var request = new PostPaymentRequest(CARD_NUMBER, expiryMonth, futureYear, CURRENCY, AMOUNT,
        CVV);
    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.message").value(
            "Rejected: expiry_month/expiry_year are required and must be in the future"));
  }

  static Stream<Arguments> processPaymentExpiryDateErrorProvider() {
    var monthAgo = now().minusMonths(1);
    var yearAgo = now().minusYears(1);
    // month, year
    return Stream.of(
        arguments(monthAgo.getMonthValue(), monthAgo.getYear()),
        arguments(yearAgo.getMonthValue(), yearAgo.getYear()));
  }

  @ParameterizedTest
  @MethodSource("processPaymentExpiryDateErrorProvider")
  void whenPaymentProcessWithWrongExpiryDateThen422IsReturned(int month, int year)
      throws Exception {
    var request = new PostPaymentRequest(CARD_NUMBER, month, year, CURRENCY, AMOUNT, CVV);
    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.message").value(
            "Rejected: expiry_month/expiry_year are required and must be in the future"));
  }

  // todo csv source for simplicity
  static Stream<Arguments> processPaymentCurrencyErrorProvider() {
    return Stream.of(
        arguments("AAA"),
        arguments("CAD"),
        arguments("A"),
        arguments(""),
        arguments("AAAA"),
        arguments((String) null));
  }

  @ParameterizedTest
  @MethodSource("processPaymentCurrencyErrorProvider")
  void whenPaymentProcessWithWrongCurrencyThen422IsReturned(String currency)
      throws Exception {
    var request = new PostPaymentRequest(CARD_NUMBER, EXPIRY_MONTH, futureYear, currency, AMOUNT,
        CVV);
    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(
            jsonPath("$.message").value(
                "Rejected: currency is required and must be one of USD, EUR, GBP"));
  }

  static Stream<Arguments> processPaymentAmountErrorProvider() {
    var positiveError = "Rejected: amount must be an integer greater than zero";
    var requiredError = "Rejected: amount is required";
    var maxError = "Rejected: JSON parse error: Numeric value (9223372036854775807) out of range of int (-2147483648 - 2147483647)";
    // amount (raw json), error message
    return Stream.of(
        arguments(-1, positiveError),
        arguments(0, positiveError),
        arguments("", requiredError),
        arguments(Long.MAX_VALUE, maxError));
  }

  @ParameterizedTest
  @MethodSource("processPaymentAmountErrorProvider")
  void whenPaymentProcessWithWrongAmountThen422IsReturned(Object amount, String errorMessage)
      throws Exception {
    var request = validPostRequest();
    ObjectNode body = objectMapper.valueToTree(request);
    body.set("amount", objectMapper.valueToTree(amount));
    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.message").value(errorMessage));
  }

  static Stream<Arguments> processPaymentCVVErrorProvider() {
    var characterError = "cvv must be 3-4 numeric characters long";
    var requiredError = "cvv is required";
    // cvv, expected errors
    return Stream.of(
        arguments("12", List.of(characterError)),
        arguments("", List.of(characterError, requiredError)),
        arguments(null, List.of(requiredError)),
        arguments("-12345678912345", List.of(characterError)),
        arguments("12345", List.of(characterError)));
  }

  @ParameterizedTest
  @MethodSource("processPaymentCVVErrorProvider")
  void whenPaymentProcessWithWrongCVVThen422IsReturned(String cvv,
      List<String> errorMessages) throws Exception {
    var request = new PostPaymentRequest(CARD_NUMBER, EXPIRY_MONTH, futureYear, CURRENCY, AMOUNT,
        cvv);
    var result = mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnprocessableEntity());
    for (var errorMessage : errorMessages) {
      result.andExpect(jsonPath("$.message", containsString(errorMessage)));
    }
  }

  @Test
  void whenPaymentProcessWithValidRequestThenDeclinedIsReturned() throws Exception {
    var request = validPostRequest();
    var bankRequest = new PostPaymentBankRequest(
        request.cardNumber(),
        request.getExpiryDate(),
        request.currency(),
        request.amount(),
        request.cvv());
    var bankResponse = new ResponseEntity<>(new PostPaymentBankResponse(false, null),
        HttpStatus.OK);
    when(bankCommunicator.sendPayment(eq(bankRequest), any())).thenReturn(bankResponse);

    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", matchesPattern(UUID_PATTERN)))
        .andExpect(jsonPath("$.status").value(PaymentStatus.DECLINED.getName()))
        .andExpect(jsonPath("$.cardNumberLastFour").value("1111"))
        .andExpect(jsonPath("$.expiryMonth").value(request.expiryMonth()))
        .andExpect(jsonPath("$.expiryYear").value(request.expiryYear()))
        .andExpect(jsonPath("$.currency").value(request.currency()))
        .andExpect(jsonPath("$.amount").value(request.amount()));
  }

  @Test
  void whenPaymentProcessWithEmptyRequestThen422IsReturned() throws Exception {
    var expectedErrors = List.of(
        "card_number is required",
        "amount is required",
        "cvv is required",
        "expiry_month/expiry_year are required and must be in the future",
        "currency is required and must be one of USD, EUR, GBP");
    var result = mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isUnprocessableEntity());
    for (var errorMessage : expectedErrors) {
      result.andExpect(jsonPath("$.message", containsString(errorMessage)));
    }
  }
}
