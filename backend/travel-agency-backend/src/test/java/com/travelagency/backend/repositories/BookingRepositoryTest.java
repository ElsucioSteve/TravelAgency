package com.travelagency.backend.repositories;

import com.travelagency.backend.entities.BookingEntity;
import com.travelagency.backend.entities.TravelPackageEntity;
import com.travelagency.backend.entities.UserEntity;
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
class BookingRepositoryTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TravelPackageRepository travelPackageRepository;

    private UserEntity user;
    private TravelPackageEntity travelPackage;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        userRepository.deleteAll();
        travelPackageRepository.deleteAll();

        // Crear un usuario
        user = new UserEntity();
        user.setEmail("juan@test.com");
        user.setFullName("Juan");
        user.setKeycloakId("kc-juan");
        user.setUserRole("CLIENT");
        user.setAccountStatus("ACTIVE");
        user = userRepository.save(user);

        // Crear un paquete
        travelPackage = new TravelPackageEntity();
        travelPackage.setName("Tour Test");
        travelPackage.setDestination("Chile");
        travelPackage.setStartDate(LocalDate.now().plusDays(30));
        travelPackage.setEndDate(LocalDate.now().plusDays(35));
        travelPackage.setBasePrice(new BigDecimal("500000"));
        travelPackage.setTotalSlots(20);
        travelPackage.setAvailableSlots(20);
        travelPackage.setTravelType("NATIONAL");
        travelPackage.setSeason("HIGH");
        travelPackage.setCategory("ADVENTURE");
        travelPackage.setPackageStatus("AVAILABLE");
        travelPackage.setIsVisibleWeb(true);
        travelPackage = travelPackageRepository.save(travelPackage);
    }

    private BookingEntity createBooking(String status) {
        BookingEntity booking = new BookingEntity();
        booking.setUser(user);
        booking.setTravelPackage(travelPackage);
        booking.setBookingDate(LocalDateTime.now());
        booking.setPassengerCount(2);
        booking.setGrossAmount(new BigDecimal("1000000"));
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setFinalAmount(new BigDecimal("1000000"));
        booking.setBookingStatus(status);
        booking.setBookingCode("BK-TEST-" + System.nanoTime());
        return bookingRepository.save(booking);
    }

    @Test
    @DisplayName("findByUserId() devuelve solo las reservas del usuario")
    void findByUserId_ShouldReturnUserBookings() {
        createBooking("PENDING");
        createBooking("CONFIRMED");

        List<BookingEntity> bookings = bookingRepository.findByUserId(user.getId());

        assertThat(bookings).hasSize(2);
    }

    @Test
    @DisplayName("findByBookingStatus() filtra por estado")
    void findByBookingStatus_ShouldFilter() {
        createBooking("PENDING");
        createBooking("PENDING");
        createBooking("CONFIRMED");

        List<BookingEntity> pending = bookingRepository.findByBookingStatus("PENDING");
        List<BookingEntity> confirmed = bookingRepository.findByBookingStatus("CONFIRMED");

        assertThat(pending).hasSize(2);
        assertThat(confirmed).hasSize(1);
    }

    @Test
    @DisplayName("countByUserIdAndBookingStatus() cuenta correctamente")
    void countByUserIdAndBookingStatus_ShouldCount() {
        createBooking("CONFIRMED");
        createBooking("CONFIRMED");
        createBooking("PENDING");

        long confirmedCount = bookingRepository.countByUserIdAndBookingStatus(user.getId(), "CONFIRMED");
        long pendingCount = bookingRepository.countByUserIdAndBookingStatus(user.getId(), "PENDING");

        assertThat(confirmedCount).isEqualTo(2);
        assertThat(pendingCount).isEqualTo(1);
    }

    @Test
    @DisplayName("findByUserIdAndBookingStatus() filtra por usuario y estado")
    void findByUserIdAndBookingStatus_ShouldFilter() {
        createBooking("PENDING");
        createBooking("PENDING");
        createBooking("CONFIRMED");

        List<BookingEntity> pending = bookingRepository.findByUserIdAndBookingStatus(user.getId(), "PENDING");

        assertThat(pending).hasSize(2);
    }

    @Test
    @DisplayName("findByBookingCode() encuentra por codigo unico")
    void findByBookingCode_ShouldFind() {
        BookingEntity saved = createBooking("PENDING");

        var found = bookingRepository.findByBookingCode(saved.getBookingCode());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    @DisplayName("findSalesByPeriod() devuelve solo CONFIRMED en rango")
    void findSalesByPeriod_ShouldReturnConfirmedInRange() {
        createBooking("CONFIRMED");
        createBooking("PENDING");  // No deberia aparecer
        createBooking("CANCELED"); // No deberia aparecer

        List<BookingEntity> sales = bookingRepository.findSalesByPeriod(
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1)
        );

        assertThat(sales).hasSize(1);
        assertThat(sales.get(0).getBookingStatus()).isEqualTo("CONFIRMED");
    }
}