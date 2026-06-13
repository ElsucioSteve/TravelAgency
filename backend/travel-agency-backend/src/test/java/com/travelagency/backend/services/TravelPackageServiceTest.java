package com.travelagency.backend.services;

import com.travelagency.backend.entities.TravelPackageEntity;
import com.travelagency.backend.repositories.TravelPackageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TravelPackageServiceTest {

    @Mock
    private TravelPackageRepository travelPackageRepository;

    @InjectMocks
    private TravelPackageService travelPackageService;

    private TravelPackageEntity samplePackage;

    @BeforeEach
    void setUp() {
        samplePackage = new TravelPackageEntity();
        samplePackage.setId(1L);
        samplePackage.setName("Test Package");
        samplePackage.setDestination("Patagonia");
        samplePackage.setStartDate(LocalDate.now().plusDays(30));
        samplePackage.setEndDate(LocalDate.now().plusDays(35));
        samplePackage.setBasePrice(new BigDecimal("500000"));
        samplePackage.setTotalSlots(20);
        samplePackage.setAvailableSlots(20);
        samplePackage.setTravelType("NATIONAL");
        samplePackage.setSeason("HIGH");
        samplePackage.setCategory("ADVENTURE");
        samplePackage.setPackageStatus("AVAILABLE");
        samplePackage.setIsVisibleWeb(true);
    }

    @Test
    @DisplayName("getAllPackages() should return all packages")
        // Devuelve todos los paquetes (admin)
    void getAllPackages_ShouldReturnAll() {
        when(travelPackageRepository.findAll()).thenReturn(List.of(samplePackage));

        List<TravelPackageEntity> result = travelPackageService.getAllPackages();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getAllVisiblePackages() should return only visible AVAILABLE packages")
        // Devuelve solo los paquetes AVAILABLE marcados como visibles (catalogo cliente)
    void getAllVisiblePackages_ShouldReturnFiltered() {
        when(travelPackageRepository.findByPackageStatusAndIsVisibleWebTrue("AVAILABLE"))
                .thenReturn(List.of(samplePackage));

        List<TravelPackageEntity> result = travelPackageService.getAllVisiblePackages();

        assertThat(result).hasSize(1);
        verify(travelPackageRepository).findByPackageStatusAndIsVisibleWebTrue("AVAILABLE");
    }

    @Test
    @DisplayName("getPackageById() should return package when it exists")
        // Busca un paquete por ID exitosamente
    void getPackageById_WhenExists_ShouldReturn() {
        when(travelPackageRepository.findById(1L)).thenReturn(Optional.of(samplePackage));

        TravelPackageEntity result = travelPackageService.getPackageById(1L);

        assertThat(result.getName()).isEqualTo("Test Package");
    }

    @Test
    @DisplayName("getPackageById() should throw when package does not exist")
        // Lanza excepcion si el paquete no existe
    void getPackageById_WhenNotExists_ShouldThrow() {
        when(travelPackageRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> travelPackageService.getPackageById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("createPackage() should auto-calculate duration if not provided")
        // Calcula automaticamente la duracion en dias entre fechas
    void createPackage_ShouldAutoCalculateDuration() {
        samplePackage.setDurationDays(null);
        when(travelPackageRepository.save(any(TravelPackageEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        travelPackageService.createPackage(samplePackage);

        assertThat(samplePackage.getDurationDays()).isEqualTo(5);
    }

    @Test
    @DisplayName("createPackage() should set availableSlots equal to totalSlots if null")
        // Por defecto, los cupos disponibles igualan a los totales
    void createPackage_ShouldSetAvailableSlots() {
        samplePackage.setAvailableSlots(null);
        when(travelPackageRepository.save(any(TravelPackageEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        travelPackageService.createPackage(samplePackage);

        assertThat(samplePackage.getAvailableSlots()).isEqualTo(20);
    }

    @Test
    @DisplayName("createPackage() should throw when basePrice is zero or negative")
        // Valida que el precio sea positivo
    void createPackage_WithInvalidPrice_ShouldThrow() {
        samplePackage.setBasePrice(BigDecimal.ZERO);

        assertThatThrownBy(() -> travelPackageService.createPackage(samplePackage))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Base price");
    }

    @Test
    @DisplayName("createPackage() should throw when totalSlots is zero or negative")
        // Valida que los cupos sean positivos
    void createPackage_WithInvalidSlots_ShouldThrow() {
        samplePackage.setTotalSlots(0);

        assertThatThrownBy(() -> travelPackageService.createPackage(samplePackage))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Total slots");
    }

    @Test
    @DisplayName("createPackage() should throw when endDate is before startDate")
        // Valida coherencia de fechas
    void createPackage_WithInvalidDates_ShouldThrow() {
        samplePackage.setEndDate(samplePackage.getStartDate().minusDays(1));

        assertThatThrownBy(() -> travelPackageService.createPackage(samplePackage))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("End date");
    }

    @Test
    @DisplayName("decrementAvailableSlots() should reduce available slots")
        // Reduce los cupos disponibles tras una reserva
    void decrementAvailableSlots_ShouldReduce() {
        when(travelPackageRepository.findById(1L)).thenReturn(Optional.of(samplePackage));
        when(travelPackageRepository.save(any(TravelPackageEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        travelPackageService.decrementAvailableSlots(1L, 5);

        assertThat(samplePackage.getAvailableSlots()).isEqualTo(15);
    }

    @Test
    @DisplayName("decrementAvailableSlots() to zero should mark package as SOLD_OUT")
        // Cuando los cupos llegan a 0, el paquete pasa a SOLD_OUT
    void decrementAvailableSlots_WhenReachesZero_ShouldMarkSoldOut() {
        when(travelPackageRepository.findById(1L)).thenReturn(Optional.of(samplePackage));
        when(travelPackageRepository.save(any(TravelPackageEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        travelPackageService.decrementAvailableSlots(1L, 20);

        assertThat(samplePackage.getAvailableSlots()).isEqualTo(0);
        assertThat(samplePackage.getPackageStatus()).isEqualTo("SOLD_OUT");
    }

    @Test
    @DisplayName("decrementAvailableSlots() should throw when not enough slots")
        // Lanza excepcion si se piden mas cupos que los disponibles
    void decrementAvailableSlots_NotEnoughSlots_ShouldThrow() {
        when(travelPackageRepository.findById(1L)).thenReturn(Optional.of(samplePackage));

        assertThatThrownBy(() -> travelPackageService.decrementAvailableSlots(1L, 25))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Not enough");
    }

    @Test
    @DisplayName("expirePassedPackages() should expire packages with past endDate")
        // Marca como EXPIRED los paquetes cuya fecha de fin ya paso
    void expirePassedPackages_ShouldExpirePast() {
        TravelPackageEntity expired = new TravelPackageEntity();
        expired.setId(2L);
        expired.setPackageStatus("AVAILABLE");
        expired.setEndDate(LocalDate.now().minusDays(1));

        TravelPackageEntity stillValid = new TravelPackageEntity();
        stillValid.setId(3L);
        stillValid.setPackageStatus("AVAILABLE");
        stillValid.setEndDate(LocalDate.now().plusDays(10));

        when(travelPackageRepository.findAll()).thenReturn(List.of(expired, stillValid));
        when(travelPackageRepository.save(any(TravelPackageEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        int count = travelPackageService.expirePassedPackages();

        assertThat(count).isEqualTo(1);
        assertThat(expired.getPackageStatus()).isEqualTo("EXPIRED");
        assertThat(stillValid.getPackageStatus()).isEqualTo("AVAILABLE");
    }

    @Test
    @DisplayName("deletePackage() with no reservations should physically delete")
        // Si el paquete no tiene reservas, se elimina fisicamente
    void deletePackage_NoReservations_ShouldDelete() {
        when(travelPackageRepository.findById(1L)).thenReturn(Optional.of(samplePackage));

        travelPackageService.deletePackage(1L);

        verify(travelPackageRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deletePackage() with reservations should mark as CANCELED")
        // Si tiene reservas, no se borra: se marca como CANCELED
    void deletePackage_WithReservations_ShouldMarkCanceled() {
        samplePackage.setAvailableSlots(15); // 5 ya reservados
        when(travelPackageRepository.findById(1L)).thenReturn(Optional.of(samplePackage));
        when(travelPackageRepository.save(any(TravelPackageEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        travelPackageService.deletePackage(1L);

        assertThat(samplePackage.getPackageStatus()).isEqualTo("CANCELED");
        assertThat(samplePackage.getIsVisibleWeb()).isFalse();
    }

    @Test
    @DisplayName("searchPackages() should delegate to repository")
        // Verifica que la busqueda con filtros se delega correctamente
    void searchPackages_ShouldDelegate() {
        when(travelPackageRepository.searchPackages(
                anyString(), any(BigDecimal.class), any(BigDecimal.class), any(LocalDate.class)
        )).thenReturn(List.of(samplePackage));

        List<TravelPackageEntity> result = travelPackageService.searchPackages(
                "Chile", new BigDecimal("100000"), new BigDecimal("1000000"), LocalDate.now()
        );

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("updatePackage() should update all editable fields")
        // Actualiza todos los campos editables del paquete
    void updatePackage_ShouldUpdateAllFields() {
        when(travelPackageRepository.findById(1L)).thenReturn(Optional.of(samplePackage));
        when(travelPackageRepository.save(any(TravelPackageEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TravelPackageEntity updates = new TravelPackageEntity();
        updates.setName("Updated Name");
        updates.setDestination("Updated Dest");
        updates.setDescription("Updated Desc");
        updates.setStartDate(LocalDate.now().plusDays(40));
        updates.setEndDate(LocalDate.now().plusDays(45));
        updates.setBasePrice(new BigDecimal("800000"));
        updates.setIncludedServices("Servicios");
        updates.setConditions("Condiciones");
        updates.setRestrictions("Restricciones");
        updates.setTravelType("INTERNATIONAL");
        updates.setSeason("LOW");
        updates.setCategory("BEACH");
        updates.setPackageStatus("AVAILABLE");
        updates.setIsVisibleWeb(false);
        updates.setTotalSlots(20);

        TravelPackageEntity result = travelPackageService.updatePackage(1L, updates);

        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getDestination()).isEqualTo("Updated Dest");
        assertThat(result.getCategory()).isEqualTo("BEACH");
    }

    @Test
    @DisplayName("updatePackage() should throw when reducing totalSlots below reserved")
        // No se puede bajar los cupos totales por debajo de los ya reservados
    void updatePackage_WhenReducingBelowReserved_ShouldThrow() {
        samplePackage.setAvailableSlots(5);  // 15 ya reservados (20 - 5)
        when(travelPackageRepository.findById(1L)).thenReturn(Optional.of(samplePackage));

        TravelPackageEntity updates = new TravelPackageEntity();
        updates.setName("Test");
        updates.setDestination("Test");
        updates.setStartDate(LocalDate.now().plusDays(30));
        updates.setEndDate(LocalDate.now().plusDays(35));
        updates.setBasePrice(new BigDecimal("500000"));
        updates.setTotalSlots(10);  // intenta bajar a 10 pero ya hay 15 reservados

        assertThatThrownBy(() -> travelPackageService.updatePackage(1L, updates))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("reduce total slots");
    }

    @Test
    @DisplayName("updatePackage() should adjust availableSlots when totalSlots changes")
        // Si suben los cupos totales, los disponibles suben proporcionalmente
    void updatePackage_WhenIncreasingTotalSlots_ShouldAdjustAvailable() {
        samplePackage.setAvailableSlots(15);  // 5 ya reservados
        when(travelPackageRepository.findById(1L)).thenReturn(Optional.of(samplePackage));
        when(travelPackageRepository.save(any(TravelPackageEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TravelPackageEntity updates = new TravelPackageEntity();
        updates.setName("Test");
        updates.setDestination("Test");
        updates.setStartDate(LocalDate.now().plusDays(30));
        updates.setEndDate(LocalDate.now().plusDays(35));
        updates.setBasePrice(new BigDecimal("500000"));
        updates.setTotalSlots(30);  // sube de 20 a 30

        TravelPackageEntity result = travelPackageService.updatePackage(1L, updates);

        // 5 reservados, ahora 30 totales -> 25 disponibles
        assertThat(result.getTotalSlots()).isEqualTo(30);
        assertThat(result.getAvailableSlots()).isEqualTo(25);
    }

    @Test
    @DisplayName("createPackage() should not override existing availableSlots if provided")
        // Si se provee availableSlots, no se sobreescribe
    void createPackage_WithExplicitAvailableSlots_ShouldRespect() {
        samplePackage.setAvailableSlots(10);  // explicito
        when(travelPackageRepository.save(any(TravelPackageEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TravelPackageEntity result = travelPackageService.createPackage(samplePackage);

        assertThat(result.getAvailableSlots()).isEqualTo(10);
    }

    @Test
    @DisplayName("createPackage() should set default packageStatus when null")
        // Estado por defecto es AVAILABLE
    void createPackage_WhenStatusNull_ShouldDefaultToAvailable() {
        samplePackage.setPackageStatus(null);
        when(travelPackageRepository.save(any(TravelPackageEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        travelPackageService.createPackage(samplePackage);

        assertThat(samplePackage.getPackageStatus()).isEqualTo("AVAILABLE");
    }

    @Test
    @DisplayName("createPackage() should set default isVisibleWeb when null")
        // Por defecto visible en web
    void createPackage_WhenIsVisibleNull_ShouldDefaultToTrue() {
        samplePackage.setIsVisibleWeb(null);
        when(travelPackageRepository.save(any(TravelPackageEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        travelPackageService.createPackage(samplePackage);

        assertThat(samplePackage.getIsVisibleWeb()).isTrue();
    }

    @Test
    @DisplayName("createPackage() should throw when basePrice is null")
        // basePrice no puede ser null
    void createPackage_WithNullPrice_ShouldThrow() {
        samplePackage.setBasePrice(null);

        assertThatThrownBy(() -> travelPackageService.createPackage(samplePackage))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Base price");
    }

    @Test
    @DisplayName("createPackage() should throw when totalSlots is null")
        // totalSlots no puede ser null
    void createPackage_WithNullSlots_ShouldThrow() {
        samplePackage.setTotalSlots(null);

        assertThatThrownBy(() -> travelPackageService.createPackage(samplePackage))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Total slots");
    }
}