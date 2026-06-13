package com.travelagency.backend.controllers;

import com.travelagency.backend.entities.DiscountConfigEntity;
import com.travelagency.backend.services.DiscountConfigService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/discounts")
public class DiscountConfigController {

    private final DiscountConfigService discountConfigService;

    public DiscountConfigController(DiscountConfigService discountConfigService) {
        this.discountConfigService = discountConfigService;
    }

    // Cualquier autenticado puede ver los descuentos activos (transparencia)
    // Si se pasa el parametro ?criteria=PROMO (o GROUP, FREQUENT, MULTI_PACKAGE),
    // se filtra solo por ese criterio.
    @GetMapping
    public List<DiscountConfigEntity> getAll(
            @RequestParam(required = false) String criteria) {
        if (criteria != null && !criteria.isEmpty()) {
            return discountConfigService.getActiveDiscountsByCriteria(criteria);
        }
        return discountConfigService.getActiveDiscounts();
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public List<DiscountConfigEntity> getAllIncludingInactive() {
        return discountConfigService.getAll();
    }

    @GetMapping("/{id}")
    public DiscountConfigEntity getById(@PathVariable Long id) {
        return discountConfigService.getById(id);
    }

    // ----- Gestion solo para ADMIN -----

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DiscountConfigEntity> create(@RequestBody DiscountConfigEntity discount) {
        return new ResponseEntity<>(discountConfigService.create(discount), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public DiscountConfigEntity update(@PathVariable Long id, @RequestBody DiscountConfigEntity discount) {
        return discountConfigService.update(id, discount);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        discountConfigService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}