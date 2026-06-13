package com.travelagency.backend.services;

import com.travelagency.backend.entities.DiscountConfigEntity;
import com.travelagency.backend.repositories.DiscountConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscountConfigServiceTest {

    @Mock
    private DiscountConfigRepository discountConfigRepository;

    @InjectMocks
    private DiscountConfigService discountConfigService;

    private DiscountConfigEntity sampleDiscount;

    @BeforeEach
    void setUp() {
        sampleDiscount = new DiscountConfigEntity();
        sampleDiscount.setId(1L);
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
    @DisplayName("getAll() should return all discounts")
        // Devuelve todos los descuentos del repositorio
    void getAll_ShouldReturnAll() {
        when(discountConfigRepository.findAll()).thenReturn(List.of(sampleDiscount));

        List<DiscountConfigEntity> result = discountConfigService.getAll();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getById() should return discount when it exists")
        // Busca un descuento por ID
    void getById_WhenExists_ShouldReturn() {
        when(discountConfigRepository.findById(1L)).thenReturn(Optional.of(sampleDiscount));

        DiscountConfigEntity result = discountConfigService.getById(1L);

        assertThat(result.getName()).isEqualTo("Group Discount");
    }

    @Test
    @DisplayName("getById() should throw when discount does not exist")
        // Lanza excepcion si el descuento no existe
    void getById_WhenNotExists_ShouldThrow() {
        when(discountConfigRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> discountConfigService.getById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("getActiveDiscounts() should call repository with ACTIVE status")
        // Lista los descuentos cuyo estado es ACTIVE
    void getActiveDiscounts_ShouldFilter() {
        when(discountConfigRepository.findByDiscountStatus("ACTIVE")).thenReturn(List.of(sampleDiscount));

        List<DiscountConfigEntity> result = discountConfigService.getActiveDiscounts();

        assertThat(result).hasSize(1);
        verify(discountConfigRepository).findByDiscountStatus("ACTIVE");
    }

    @Test
    @DisplayName("getActiveDiscountsByCriteria() should filter by criteria AND validity range")
        // Filtra por criterio (ej PROMO) y por estar dentro del rango de vigencia
    void getActiveDiscountsByCriteria_ShouldFilter() {
        when(discountConfigRepository.findByApplicationCriteriaAndDiscountStatus("GROUP", "ACTIVE"))
                .thenReturn(List.of(sampleDiscount));

        List<DiscountConfigEntity> result = discountConfigService.getActiveDiscountsByCriteria("GROUP");

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getActiveDiscountsByCriteria() should exclude discounts outside validity range")
        // Excluye descuentos cuya vigencia ya termino o aun no comienza
    void getActiveDiscountsByCriteria_ShouldExcludeOutOfRange() {
        DiscountConfigEntity expired = new DiscountConfigEntity();
        expired.setApplicationCriteria("GROUP");
        expired.setDiscountStatus("ACTIVE");
        expired.setStartDate(LocalDateTime.now().minusDays(60));
        expired.setEndDate(LocalDateTime.now().minusDays(30));

        DiscountConfigEntity future = new DiscountConfigEntity();
        future.setApplicationCriteria("GROUP");
        future.setDiscountStatus("ACTIVE");
        future.setStartDate(LocalDateTime.now().plusDays(30));
        future.setEndDate(LocalDateTime.now().plusDays(60));

        when(discountConfigRepository.findByApplicationCriteriaAndDiscountStatus("GROUP", "ACTIVE"))
                .thenReturn(List.of(sampleDiscount, expired, future));

        List<DiscountConfigEntity> result = discountConfigService.getActiveDiscountsByCriteria("GROUP");

        // Solo sampleDiscount esta vigente
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("isWithinValidDateRange() should return true for a discount in range")
        // Verifica el helper de rango de fechas para un descuento vigente
    void isWithinValidDateRange_ShouldReturnTrue() {
        boolean result = discountConfigService.isWithinValidDateRange(sampleDiscount);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isWithinValidDateRange() should return false for expired discount")
        // Verifica que detecta un descuento expirado
    void isWithinValidDateRange_WhenExpired_ShouldReturnFalse() {
        sampleDiscount.setEndDate(LocalDateTime.now().minusDays(1));

        boolean result = discountConfigService.isWithinValidDateRange(sampleDiscount);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("create() should save the discount with default values")
        // Crea un descuento y aplica valores por defecto
    void create_ShouldSaveWithDefaults() {
        sampleDiscount.setDiscountStatus(null);
        sampleDiscount.setIsStackable(null);
        when(discountConfigRepository.save(any(DiscountConfigEntity.class))).thenReturn(sampleDiscount);

        discountConfigService.create(sampleDiscount);

        // Por defecto el estado pasa a ACTIVE e isStackable a false
        assertThat(sampleDiscount.getDiscountStatus()).isEqualTo("ACTIVE");
        assertThat(sampleDiscount.getIsStackable()).isFalse();
    }

    @Test
    @DisplayName("create() should throw with invalid valueType")
        // Rechaza tipos de valor invalidos
    void create_WithInvalidValueType_ShouldThrow() {
        sampleDiscount.setValueType("INVALID");

        assertThatThrownBy(() -> discountConfigService.create(sampleDiscount))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid valueType");
    }

    @Test
    @DisplayName("create() should throw when percentage is greater than 100")
        // Rechaza porcentajes superiores al 100%
    void create_WithPercentageOver100_ShouldThrow() {
        sampleDiscount.setDiscountValue(new BigDecimal("150"));

        assertThatThrownBy(() -> discountConfigService.create(sampleDiscount))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("100%");
    }

    @Test
    @DisplayName("create() should throw when discountValue is zero or negative")
        // Rechaza valores cero o negativos
    void create_WithZeroValue_ShouldThrow() {
        sampleDiscount.setDiscountValue(BigDecimal.ZERO);

        assertThatThrownBy(() -> discountConfigService.create(sampleDiscount))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    @DisplayName("create() should throw when endDate is before startDate")
        // Rechaza fechas de fin anteriores al inicio
    void create_WithInvalidDateRange_ShouldThrow() {
        sampleDiscount.setStartDate(LocalDateTime.now().plusDays(10));
        sampleDiscount.setEndDate(LocalDateTime.now().plusDays(5));

        assertThatThrownBy(() -> discountConfigService.create(sampleDiscount))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("End date");
    }

    @Test
    @DisplayName("update() should modify and save the existing discount")
        // Actualiza campos existentes
    void update_ShouldModify() {
        when(discountConfigRepository.findById(1L)).thenReturn(Optional.of(sampleDiscount));
        when(discountConfigRepository.save(any(DiscountConfigEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DiscountConfigEntity updates = new DiscountConfigEntity();
        updates.setName("Updated Name");
        updates.setDiscountValue(new BigDecimal("15"));

        DiscountConfigEntity result = discountConfigService.update(1L, updates);

        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getDiscountValue()).isEqualByComparingTo("15");
    }

    @Test
    @DisplayName("delete() should soft delete by setting status to INACTIVE")
        // Soft delete: cambia el estado a INACTIVE, no elimina fisicamente
    void delete_ShouldSoftDelete() {
        when(discountConfigRepository.findById(1L)).thenReturn(Optional.of(sampleDiscount));
        when(discountConfigRepository.save(any(DiscountConfigEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        discountConfigService.delete(1L);

        assertThat(sampleDiscount.getDiscountStatus()).isEqualTo("INACTIVE");
        verify(discountConfigRepository).save(sampleDiscount);
    }
}