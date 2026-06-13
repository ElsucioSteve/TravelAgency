package com.travelagency.backend.repositories;

import com.travelagency.backend.entities.DiscountConfigEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase
@ActiveProfiles("test")
class DiscountConfigRepositoryTest {

    @Autowired
    private DiscountConfigRepository discountConfigRepository;

    private DiscountConfigEntity sampleDiscount;

    @BeforeEach
    void setUp() {
        discountConfigRepository.deleteAll();

        // Descuento de grupo activo y vigente
        sampleDiscount = new DiscountConfigEntity();
        sampleDiscount.setName("Group Discount");
        sampleDiscount.setApplicationCriteria("GROUP");
        sampleDiscount.setValueType("PERCENTAGE");
        sampleDiscount.setDiscountValue(new BigDecimal("10"));
        sampleDiscount.setDiscountStatus("ACTIVE");
        sampleDiscount.setIsStackable(true);
        sampleDiscount.setStartDate(LocalDateTime.now().minusDays(10));
        sampleDiscount.setEndDate(LocalDateTime.now().plusDays(30));
    }

    @Test
    @DisplayName("save() should persist the discount and assign an ID")
        // Verifica que al guardar un descuento, se persiste y obtiene un ID
    void save_ShouldPersistAndAssignId() {
        DiscountConfigEntity saved = discountConfigRepository.save(sampleDiscount);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Group Discount");
    }

    @Test
    @DisplayName("findById() should return the discount when it exists")
        // Busca un descuento por ID y verifica que se obtiene
    void findById_WhenExists_ShouldReturnDiscount() {
        DiscountConfigEntity saved = discountConfigRepository.save(sampleDiscount);

        Optional<DiscountConfigEntity> found = discountConfigRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getApplicationCriteria()).isEqualTo("GROUP");
    }

    @Test
    @DisplayName("findByDiscountStatus() should filter by status")
        // Filtra descuentos por estado: ACTIVE vs INACTIVE
    void findByDiscountStatus_ShouldFilter() {
        discountConfigRepository.save(sampleDiscount);

        DiscountConfigEntity inactive = createDiscount("Inactive", "PROMO", "INACTIVE");
        discountConfigRepository.save(inactive);

        List<DiscountConfigEntity> active = discountConfigRepository.findByDiscountStatus("ACTIVE");
        List<DiscountConfigEntity> notActive = discountConfigRepository.findByDiscountStatus("INACTIVE");

        assertThat(active).hasSize(1);
        assertThat(notActive).hasSize(1);
    }

    @Test
    @DisplayName("findByApplicationCriteriaAndDiscountStatus() should filter by criteria and status")
        // Filtra por criterio Y estado: solo PROMO activos
    void findByApplicationCriteriaAndDiscountStatus_ShouldFilter() {
        discountConfigRepository.save(sampleDiscount);

        DiscountConfigEntity promo = createDiscount("Promo", "PROMO", "ACTIVE");
        discountConfigRepository.save(promo);

        DiscountConfigEntity promoInactive = createDiscount("Promo Off", "PROMO", "INACTIVE");
        discountConfigRepository.save(promoInactive);

        List<DiscountConfigEntity> activePromos = discountConfigRepository
                .findByApplicationCriteriaAndDiscountStatus("PROMO", "ACTIVE");

        assertThat(activePromos).hasSize(1);
        assertThat(activePromos.get(0).getName()).isEqualTo("Promo");
    }

    @Test
    @DisplayName("findAll() should return all discounts")
        // Trae todos los descuentos sin filtros
    void findAll_ShouldReturnAll() {
        discountConfigRepository.save(sampleDiscount);
        discountConfigRepository.save(createDiscount("Another", "PROMO", "ACTIVE"));

        List<DiscountConfigEntity> all = discountConfigRepository.findAll();

        assertThat(all).hasSize(2);
    }

    @Test
    @DisplayName("deleteById() should remove the discount")
        // Elimina un descuento por su ID
    void deleteById_ShouldRemove() {
        DiscountConfigEntity saved = discountConfigRepository.save(sampleDiscount);

        discountConfigRepository.deleteById(saved.getId());

        assertThat(discountConfigRepository.findById(saved.getId())).isEmpty();
    }

    // Helper para crear descuentos
    private DiscountConfigEntity createDiscount(String name, String criteria, String status) {
        DiscountConfigEntity d = new DiscountConfigEntity();
        d.setName(name);
        d.setApplicationCriteria(criteria);
        d.setValueType("PERCENTAGE");
        d.setDiscountValue(new BigDecimal("10"));
        d.setDiscountStatus(status);
        d.setIsStackable(false);
        d.setStartDate(LocalDateTime.now().minusDays(5));
        d.setEndDate(LocalDateTime.now().plusDays(5));
        return d;
    }
}