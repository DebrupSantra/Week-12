package com.acme.ecommerce.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    private UUID id;
    @Column(nullable = false, unique = true)
    private String sku;
    @Column(nullable = false)
    private String name;
    private String description;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    protected Product() {}

    public Product(String sku, String name, String description, BigDecimal price) {
        this.id = UUID.randomUUID();
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.price = price;
    }

    public UUID getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public void update(String name, String description, BigDecimal price) {
        this.name = name;
        this.description = description;
        this.price = price;
    }

    public record CreateProductRequest(
            @NotBlank String sku,
            @NotBlank String name,
            String description,
            @NotNull @DecimalMin(value = "0.01") BigDecimal price) {}

    public record UpdateProductRequest(@NotBlank String name, String description,
                                       @NotNull @DecimalMin(value = "0.01") BigDecimal price) {}
}
