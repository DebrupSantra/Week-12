package com.acme.ecommerce.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentRepository payments;
    public PaymentController(PaymentRepository payments) { this.payments = payments; }

    @PostMapping("/authorize")
    @Transactional
    public Payment authorize(@Valid @RequestBody AuthorizeRequest request) {
        Payment existing = payments.findByOrderId(request.orderId()).orElse(null);
        if (existing != null) {
            if (!existing.getCustomerId().equals(request.customerId())
                    || existing.getAmount().compareTo(request.amount()) != 0
                    || !"AUTHORIZED".equals(existing.getStatus())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment authorization is not repeatable");
            }
            return existing;
        }
        return payments.save(new Payment(request.orderId(), request.customerId(), request.amount()));
    }

    @PostMapping("/{orderId}/refund")
    @Transactional
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.NO_CONTENT)
    public void refund(@PathVariable UUID orderId) {
        Payment payment = payments.findByOrderId(orderId).orElse(null);
        if (payment == null) return;
        payment.refund();
        payments.save(payment);
    }

    public record AuthorizeRequest(@NotNull UUID orderId, @NotBlank String customerId,
                                   @NotNull @DecimalMin(value = "0.01") BigDecimal amount) {}
}
