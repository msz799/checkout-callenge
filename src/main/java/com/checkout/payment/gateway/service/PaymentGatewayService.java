package com.checkout.payment.gateway.service;

import static com.checkout.payment.gateway.enums.PaymentStatus.AUTHORIZED;
import static com.checkout.payment.gateway.enums.PaymentStatus.DECLINED;

import com.checkout.payment.gateway.exception.BankUnavailableException;
import com.checkout.payment.gateway.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.model.PostPaymentBankRequest;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);

  private final PaymentsRepository paymentsRepository;
  private final BankCommunicator bankCommunicator;

  public PaymentGatewayService(PaymentsRepository paymentsRepository,
      BankCommunicator bankCommunicator) {
    this.paymentsRepository = paymentsRepository;
    this.bankCommunicator = bankCommunicator;
  }

  public PostPaymentResponse getPayment(UUID id) {
    LOG.info("Requesting access to payment with ID {}", id);
    return paymentsRepository.get(id).orElseThrow(() -> new PaymentNotFoundException(id));
  }

  public PostPaymentResponse processPayment(PostPaymentRequest paymentRequest) {
    var id = UUID.randomUUID();
    var cardNumberLastFour = paymentRequest.cardNumber().substring(paymentRequest.cardNumber().length() - 4);
    LOG.info("Processing payment with ID={} card_number={}", id, cardNumberLastFour);
    var bankRequest = new PostPaymentBankRequest(
        paymentRequest.cardNumber(),
        paymentRequest.getExpiryDate(),
        paymentRequest.currency(),
        paymentRequest.amount(),
        paymentRequest.cvv()
    );
    var bankResponse = bankCommunicator.sendPayment(bankRequest, id);

    var body = bankResponse.getBody();
    if (!HttpStatus.OK.equals(bankResponse.getStatusCode()) || body == null) {
      throw new BankUnavailableException(
          "Unexpected bank response: " + bankResponse.getStatusCode(), id);
    }
    var paymentStatus = body.authorized() ? AUTHORIZED : DECLINED;
    var response =  new PostPaymentResponse(
        id,
        paymentStatus.getName(),
        cardNumberLastFour,
        paymentRequest.expiryMonth(),
        paymentRequest.expiryYear(),
        paymentRequest.currency(),
        paymentRequest.amount()
    );
    paymentsRepository.add(response);
    return response;
  }
}
