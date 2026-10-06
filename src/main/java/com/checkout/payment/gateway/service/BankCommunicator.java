package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.exception.BankUnavailableException;
import com.checkout.payment.gateway.model.PostPaymentBankRequest;
import com.checkout.payment.gateway.model.PostPaymentBankResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.UUID;

/**
 * Handles communication with the acquiring bank
 */
@Service
public class BankCommunicator {

  private static final Logger LOG = LoggerFactory.getLogger(BankCommunicator.class);
  private final RestTemplate restTemplate;
  private final String url;

  public BankCommunicator(RestTemplate restTemplate, @Value("${bank.url}") String bankUrl) {
    this.restTemplate = restTemplate;
    this.url = bankUrl;
  }

  public ResponseEntity<PostPaymentBankResponse> sendPayment(PostPaymentBankRequest request, UUID id) {
    LOG.info("Sending payment request with ID {} to acquiring bank", id);
    try {
      return restTemplate.postForEntity(url, request, PostPaymentBankResponse.class);
    } catch (Exception e) {
      throw new BankUnavailableException(e.getMessage(), id, e);
    }
  }
}
