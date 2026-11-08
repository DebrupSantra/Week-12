package com.acme.ecommerce.order;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", path = "/api/products")
public interface ProductClient {
    @GetMapping("/{id}")
    ProductResponse get(@PathVariable("id") UUID id);

    record ProductResponse(UUID id, String sku, String name, BigDecimal price) {}
}
