package com.travelagency.backend.repositories;

import com.travelagency.backend.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase
@ActiveProfiles("test")
class BookingPassengerRepositoryTest {

    @Autowired
    private BookingPassengerRepository bookingPassengerRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TravelPackageRepository travelPackageRepository;

    private BookingEntity booking;

    @BeforeEach
    void setUp() {
        bookingPassengerRepository.deleteAll();
        bookingRepository.deleteAll();
        userRepository.deleteAll();
        travelPackageRepository.deleteAll();

        UserEntity user = new UserEntity();
        user.setEmail("pax@test.com");
        user.setFullName("Pax User");
        user.setKeycloakId("kc-pax");
        user.setUserRole("CLIENT");
        user.setAccountStatus("ACTIVE");
        user = userRepository.save(user);

        TravelPackageEntity pkg = new TravelPackageEntity();
        pkg.setName("Tour");
        pkg.setDestination("Chile");
        pkg.setStartDate(LocalDate.now().plusDays(10));
        pkg.setEndDate(LocalDate.now().plusDays(15));
        pkg.setBasePrice(new BigDecimal("500000"));
        pkg.setTotalSlots(10);
        pkg.setAvailableSlots(10);
        pkg.setTravelType("NATIONAL");
        pkg.setSeason("HIGH");
        pkg.setCategory("ADVENTURE");
        pkg.setPackageStatus("AVAILABLE");
        pkg.setIsVisibleWeb(true);
        pkg = travelPackageRepository.save(pkg);

        booking = new BookingEntity();
        booking.setUser(user);
        booking.setTravelPackage(pkg);
        booking.setBookingDate(LocalDateTime.now());
        booking.setPassengerCount(2);
        booking.setGrossAmount(new BigDecimal("1000000"));
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setFinalAmount(new BigDecimal("1000000"));
        booking.setBookingStatus("PENDING");
        booking.setBookingCode("BK-PAX-001");
        booking = bookingRepository.save(booking);
    }

    @Test
    @DisplayName("save() should persist a passenger")
        // Guarda un pasajero asociado a una reserva
    void save_ShouldPersist() {
        BookingPassengerEntity passenger = new BookingPassengerEntity();
        passenger.setBooking(booking);
        passenger.setFullName("Pasajero Test");
        passenger.setIdDocument("12345678-9");
        passenger.setBirthDate(LocalDate.of(1990, 1, 1));
        passenger.setNationality("Chilena");

        BookingPassengerEntity saved = bookingPassengerRepository.save(passenger);

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    @DisplayName("findAll() should return all passengers")
        // Verifica que se pueden listar todos los pasajeros
    void findAll_ShouldReturnAll() {
        BookingPassengerEntity p1 = new BookingPassengerEntity();
        p1.setBooking(booking);
        p1.setFullName("Pasajero 1");

        BookingPassengerEntity p2 = new BookingPassengerEntity();
        p2.setBooking(booking);
        p2.setFullName("Pasajero 2");

        bookingPassengerRepository.save(p1);
        bookingPassengerRepository.save(p2);

        List<BookingPassengerEntity> all = bookingPassengerRepository.findAll();
        assertThat(all).hasSize(2);
    }
}