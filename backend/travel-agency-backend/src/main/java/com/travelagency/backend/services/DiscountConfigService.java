package com.travelagency.backend.services;

import com.travelagency.backend.entities.DiscountConfigEntity;
import com.travelagency.backend.repositories.DiscountConfigRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DiscountConfigService {

    private final DiscountConfigRepository discountConfigRepository;

    public DiscountConfigService(DiscountConfigRepository discountConfigRepository) {
        this.discountConfigRepository = discountConfigRepository;
    }

    // ----- LECTURAS -----

    public List<DiscountConfigEntity> getAll() {
        return discountConfigRepository.findAll();
    }

    public DiscountConfigEntity getById(Long id) {
        return discountConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Discount not found with id: " + id));
    }

    public List<DiscountConfigEntity> getActiveDiscounts() {
        return discountConfigRepository.findByDiscountStatus("ACTIVE");
    }

    // Devuelve descuentos ACTIVOS de un tipo especifico, validando vigencia
    public List<DiscountConfigEntity> getActiveDiscountsByCriteria(String criteria) {
        return discountConfigRepository.findByApplicationCriteriaAndDiscountStatus(criteria, "ACTIVE")
                .stream()
                .filter(this::isWithinValidDateRange)
                .toList();
    }

    // ----- ESCRITURAS (solo ADMIN) -----

    public DiscountConfigEntity create(DiscountConfigEntity discount) {
        validate(discount);

        if (discount.getDiscountStatus() == null) {
            discount.setDiscountStatus("ACTIVE");
        }
        if (discount.getIsStackable() == null) {
            discount.setIsStackable(false);
        }
        return discountConfigRepository.save(discount);
    }

    public DiscountConfigEntity update(Long id, DiscountConfigEntity updated) {
        DiscountConfigEntity existing = getById(id);

        if (updated.getName() != null) existing.setName(updated.getName());
        if (updated.getDescription() != null) existing.setDescription(updated.getDescription());
        if (updated.getValueType() != null) existing.setValueType(updated.getValueType());
        if (updated.getDiscountValue() != null) existing.setDiscountValue(updated.getDiscountValue());
        if (updated.getMaxLimit() != null) existing.setMaxLimit(updated.getMaxLimit());
        if (updated.getDiscountStatus() != null) existing.setDiscountStatus(updated.getDiscountStatus());
        if (updated.getStartDate() != null) existing.setStartDate(updated.getStartDate());
        if (updated.getEndDate() != null) existing.setEndDate(updated.getEndDate());
        if (updated.getApplicationCriteria() != null) existing.setApplicationCriteria(updated.getApplicationCriteria());
        if (updated.getIsStackable() != null) existing.setIsStackable(updated.getIsStackable());

        validate(existing);
        return discountConfigRepository.save(existing);
    }

    public void delete(Long id) {
        // Soft delete: en lugar de borrar, marcamos como INACTIVE
        DiscountConfigEntity existing = getById(id);
        existing.setDiscountStatus("INACTIVE");
        discountConfigRepository.save(existing);
    }

    // ----- VALIDACIONES Y UTILIDADES -----

    private void validate(DiscountConfigEntity d) {
        if (!"PERCENTAGE".equals(d.getValueType()) && !"FIXED_AMOUNT".equals(d.getValueType())) {
            throw new RuntimeException("Invalid valueType. Must be PERCENTAGE or FIXED_AMOUNT");
        }
        if (d.getDiscountValue() == null || d.getDiscountValue().doubleValue() <= 0) {
            throw new RuntimeException("Discount value must be greater than zero");
        }
        if ("PERCENTAGE".equals(d.getValueType()) && d.getDiscountValue().doubleValue() > 100) {
            throw new RuntimeException("Percentage discount cannot exceed 100%");
        }
        if (d.getStartDate() != null && d.getEndDate() != null && d.getEndDate().isBefore(d.getStartDate())) {
            throw new RuntimeException("End date must be after start date");
        }
    }

    public boolean isWithinValidDateRange(DiscountConfigEntity d) {
        LocalDateTime now = LocalDateTime.now();
        boolean afterStart = d.getStartDate() == null || !now.isBefore(d.getStartDate());
        boolean beforeEnd = d.getEndDate() == null || !now.isAfter(d.getEndDate());
        return afterStart && beforeEnd;
    }
}