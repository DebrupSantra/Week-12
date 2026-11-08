package com.acme.ecommerce.order;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "inventory-service", path = "/api/inventory")
public interface InventoryClient {
    @PostMapping("/reservations")
    void reserve(@RequestBody FulfillmentClient.ReservationRequest request);

    @DeleteMapping("/reservations/{orderId}")
    void release(@PathVariable("orderId") UUID orderId);
}
