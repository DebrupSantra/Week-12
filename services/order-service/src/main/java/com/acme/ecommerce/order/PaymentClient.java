package com.acme.ecommerce.order;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-service", path = "/api/payments")
public interface PaymentClient {
    @PostMapping("/authorize")
    void authorize(@RequestBody FulfillmentClient.PaymentRequest request);

    @PostMapping("/{orderId}/refund")
    void refund(@PathVariable("orderId") UUID orderId);
}
