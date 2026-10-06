package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.util.stream.Collectors;

@ControllerAdvice
public class CommonExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(CommonExceptionHandler.class);

  @ExceptionHandler(PaymentNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleException(PaymentNotFoundException ex) {
    LOG.error("Payment with ID {} not found: ", ex.getPaymentId(), ex);
    return new ResponseEntity<>(new ErrorResponse("Payment not found"), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleException(MethodArgumentNotValidException ex) {
    LOG.error("Failed to process payment: ", ex);
    var message = ex.getBindingResult().getFieldErrors().stream()
        .map(FieldError::getDefaultMessage)
        .collect(Collectors.joining(", "));
    message = "%s: %s".formatted(PaymentStatus.REJECTED.getName(), message);
    return new ResponseEntity<>(new ErrorResponse(message), HttpStatus.UNPROCESSABLE_ENTITY);
  }

  // HttpMessageNotReadableException raised when the request body cannot be deserialized at all
  // e.g. a numeric value that does not fit the target type
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleException(HttpMessageNotReadableException ex) {
    LOG.error("Failed to read payment request: ", ex);
    var message = "%s: %s".formatted(PaymentStatus.REJECTED.getName(), ex.getMessage());
    return new ResponseEntity<>(new ErrorResponse(message), HttpStatus.UNPROCESSABLE_ENTITY);
  }

  @ExceptionHandler(BankUnavailableException.class)
  public ResponseEntity<ErrorResponse> handleException(BankUnavailableException ex) {
    LOG.error("Failed to process payment with ID {}, acquiring bank is unavailable: ", ex.getPaymentId(), ex);
    return new ResponseEntity<>(new ErrorResponse("Acquiring bank is unavailable"), HttpStatus.SERVICE_UNAVAILABLE);
  }
}
