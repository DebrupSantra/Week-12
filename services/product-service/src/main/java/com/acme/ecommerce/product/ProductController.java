package com.acme.ecommerce.product;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping({"/api/products", "/api/v1/products"})
public class ProductController {
    private final ProductRepository products;

    public ProductController(ProductRepository products) { this.products = products; }

    @GetMapping
    public List<Product> list() { return products.findAll(); }

    @GetMapping("/search")
    public List<Product> search(@RequestParam String q) {
        if (q.isBlank()) return List.of();
        return products.findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(q.trim(), q.trim());
    }

    @GetMapping("/{id}")
    @Cacheable(cacheNames = "products", key = "#id")
    public Product get(@PathVariable UUID id) {
        return products.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @CacheEvict(cacheNames = "products", allEntries = true)
    public Product create(@Valid @RequestBody Product.CreateProductRequest request) {
        if (products.findBySku(request.sku()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "SKU already exists");
        }
        return products.save(new Product(request.sku(), request.name(), request.description(), request.price()));
    }

    @PutMapping("/{id}")
    @CacheEvict(cacheNames = "products", allEntries = true)
    public Product update(@PathVariable UUID id, @Valid @RequestBody Product.UpdateProductRequest request) {
        Product product = products.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        product.update(request.name(), request.description(), request.price());
        return products.save(product);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @CacheEvict(cacheNames = "products", allEntries = true)
    public void delete(@PathVariable UUID id) {
        if (!products.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }
        products.deleteById(id);
    }
}
