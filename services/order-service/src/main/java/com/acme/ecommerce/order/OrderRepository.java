package com.acme.ecommerce.order;

import java.util.UUID;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByCustomerIdOrderByCreatedAtDesc(String customerId);
    Optional<Order> findByIdAndCustomerId(UUID id, String customerId);
}
