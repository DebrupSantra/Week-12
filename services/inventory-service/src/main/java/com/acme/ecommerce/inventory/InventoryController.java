package com.acme.ecommerce.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final StockItemRepository stock;
    private final ReservationRepository reservations;
    public InventoryController(StockItemRepository stock, ReservationRepository reservations) {
        this.stock = stock;
        this.reservations = reservations;
    }

    @GetMapping("/stock")
    public List<StockItem> list() { return stock.findAll(); }

    @PostMapping("/stock")
    @Transactional
    public StockItem addStock(@Valid @RequestBody StockRequest request) {
        StockItem item = stock.findBySkuForUpdate(request.sku()).orElseGet(
                () -> stock.save(new StockItem(request.sku(), 0)));
        item.add(request.quantity());
        return stock.save(item);
    }

    @PostMapping("/reservations")
    @Transactional
    public Reservation reserve(@Valid @RequestBody ReserveRequest request) {
        Reservation existing = reservations.findById(request.orderId()).orElse(null);
        if (existing != null) {
            if (!existing.getSku().equals(request.sku()) || existing.getQuantity() != request.quantity()
                    || !"RESERVED".equals(existing.getStatus())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Order reservation is not repeatable");
            }
            return existing;
        }
        StockItem item = stock.findBySkuForUpdate(request.sku())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SKU not found"));
        if (item.getAvailable() < request.quantity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock");
        }
        item.reserve(request.quantity());
        stock.save(item);
        return reservations.save(new Reservation(request.orderId(), request.sku(), request.quantity()));
    }

    @DeleteMapping("/reservations/{orderId}")
    @Transactional
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(@PathVariable UUID orderId) {
        Reservation reservation = reservations.findById(orderId).orElse(null);
        if (reservation == null) return;
        if ("RESERVED".equals(reservation.getStatus())) {
            StockItem item = stock.findBySkuForUpdate(reservation.getSku())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SKU not found"));
            item.add(reservation.getQuantity());
            reservation.release();
            stock.save(item);
            reservations.save(reservation);
        }
    }

    public record StockRequest(@NotBlank String sku, @Min(1) int quantity) {}
    public record ReserveRequest(@NotNull UUID orderId, @NotBlank String sku, @Min(1) int quantity) {}
}
