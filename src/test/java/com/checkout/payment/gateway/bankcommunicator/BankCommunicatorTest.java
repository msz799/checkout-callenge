package com.checkout.payment.gateway.bankcommunicator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.exception.BankUnavailableException;
import com.checkout.payment.gateway.model.PostPaymentBankRequest;
import com.checkout.payment.gateway.model.PostPaymentBankResponse;
import com.checkout.payment.gateway.service.BankCommunicator;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
@AutoConfigureMockMvc
class BankCommunicatorTest {

  @Autowired
  BankCommunicator bankCommunicator;
  @MockBean
  RestTemplate restTemplate;
  @Value("${bank.url}")
  String bankUrl;

  private static final PostPaymentBankRequest BANK_REQUEST =
      new PostPaymentBankRequest("2222405343248877", "04/2030", "GBP", 100, "123");

  @Test
  void whenBankReturnsResultThenForwardIt() {
    var bankResponse = new ResponseEntity<>(
        new PostPaymentBankResponse(true, "0bb07405-6d44-4b50-a14f-7ae0beff13ad"), HttpStatus.OK);
    when(restTemplate.postForEntity(eq(bankUrl), eq(BANK_REQUEST),
        eq(PostPaymentBankResponse.class))).thenReturn(bankResponse);
    assertEquals(bankResponse, bankCommunicator.sendPayment(BANK_REQUEST, UUID.randomUUID()));
  }

  @Test
  void whenBankErrorsThenThrow() {
    var paymentId = UUID.randomUUID();
    var bankError = new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE);
    when(restTemplate.postForEntity(eq(bankUrl), eq(BANK_REQUEST),
        eq(PostPaymentBankResponse.class))).thenThrow(bankError);
    var exception = assertThrows(BankUnavailableException.class,
        () -> bankCommunicator.sendPayment(BANK_REQUEST, paymentId));
    assertEquals(paymentId, exception.getPaymentId());
    assertEquals(bankError, exception.getCause());
  }
}
