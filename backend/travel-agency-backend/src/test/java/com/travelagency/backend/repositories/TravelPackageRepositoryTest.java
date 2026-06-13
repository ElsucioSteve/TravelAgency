package com.travelagency.backend.repositories;

import com.travelagency.backend.entities.TravelPackageEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase
@ActiveProfiles("test")
class TravelPackageRepositoryTest {

    @Autowired
    private TravelPackageRepository travelPackageRepository;

    private TravelPackageEntity samplePackage;

    @BeforeEach
    void setUp() {
        travelPackageRepository.deleteAll();

        samplePackage = new TravelPackageEntity();
        samplePackage.setName("Test Package");
        samplePackage.setDestination("Patagonia, Chile");
        samplePackage.setDescription("Trekking de prueba");
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
    @DisplayName("save() debe persistir un paquete")
    void save_ShouldPersist() {
        TravelPackageEntity saved = travelPackageRepository.save(samplePackage);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Test Package");
    }

    @Test
    @DisplayName("findById() existente debe devolver el paquete")
    void findById_WhenExists_ShouldReturnPackage() {
        TravelPackageEntity saved = travelPackageRepository.save(samplePackage);

        Optional<TravelPackageEntity> found = travelPackageRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getDestination()).isEqualTo("Patagonia, Chile");
    }

    @Test
    @DisplayName("findAll() debe devolver todos los paquetes")
    void findAll_ShouldReturnAll() {
        travelPackageRepository.save(samplePackage);

        TravelPackageEntity p2 = new TravelPackageEntity();
        p2.setName("Package 2");
        p2.setDestination("Cancun");
        p2.setStartDate(LocalDate.now().plusDays(20));
        p2.setEndDate(LocalDate.now().plusDays(27));
        p2.setBasePrice(new BigDecimal("800000"));
        p2.setTotalSlots(15);
        p2.setAvailableSlots(15);
        p2.setTravelType("INTERNATIONAL");
        p2.setSeason("HIGH");
        p2.setCategory("BEACH");
        p2.setPackageStatus("AVAILABLE");
        p2.setIsVisibleWeb(true);
        travelPackageRepository.save(p2);

        List<TravelPackageEntity> all = travelPackageRepository.findAll();
        assertThat(all).hasSize(2);
    }

    @Test
    @DisplayName("delete() debe eliminar el paquete")
    void delete_ShouldRemove() {
        TravelPackageEntity saved = travelPackageRepository.save(samplePackage);
        Long id = saved.getId();

        travelPackageRepository.deleteById(id);

        assertThat(travelPackageRepository.findById(id)).isEmpty();
    }
}