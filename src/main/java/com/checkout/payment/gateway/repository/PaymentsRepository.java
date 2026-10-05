package com.checkout.payment.gateway.repository;

import com.checkout.payment.gateway.model.PostPaymentResponse;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentsRepository {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentsRepository.class);
  private final HashMap<UUID, PostPaymentResponse> payments = new HashMap<>();

  public void add(PostPaymentResponse payment) {
    LOG.info("Adding payment with ID {} to storage", payment.id());
    payments.put(payment.id(), payment);
  }

  public Optional<PostPaymentResponse> get(UUID id) {
    LOG.info("Getting payment with ID {} from storage", id);
    return Optional.ofNullable(payments.get(id));
  }
}
