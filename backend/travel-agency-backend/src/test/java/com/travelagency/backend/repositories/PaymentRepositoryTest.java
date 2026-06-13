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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase
@ActiveProfiles("test")
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TravelPackageRepository travelPackageRepository;

    private BookingEntity booking;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        bookingRepository.deleteAll();
        userRepository.deleteAll();
        travelPackageRepository.deleteAll();

        UserEntity user = new UserEntity();
        user.setEmail("test@test.com");
        user.setFullName("Test User");
        user.setKeycloakId("kc-test");
        user.setUserRole("CLIENT");
        user.setAccountStatus("ACTIVE");
        user = userRepository.save(user);

        TravelPackageEntity pkg = new TravelPackageEntity();
        pkg.setName("Test Pkg");
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
        booking.setPassengerCount(1);
        booking.setGrossAmount(new BigDecimal("500000"));
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setFinalAmount(new BigDecimal("500000"));
        booking.setBookingStatus("PENDING");
        booking.setBookingCode("BK-TEST-001");
        booking = bookingRepository.save(booking);
    }

    private PaymentEntity createPayment(String txnId) {
        PaymentEntity payment = new PaymentEntity();
        payment.setBooking(booking);
        payment.setAmountPaid(new BigDecimal("500000"));
        payment.setPaymentDate(LocalDateTime.now());
        payment.setPaymentMethod("CREDIT_CARD");
        payment.setPaymentStatus("APPROVED");
        payment.setMaskedCardNumber("**** **** **** 1234");
        payment.setCardExpirationDate("12/28");
        payment.setSimulatedCvv("***");
        payment.setInternalTransactionId(txnId);
        return payment;
    }

    @Test
    @DisplayName("save() should persist a payment")
        // Guarda un pago y verifica que recibe un ID
    void save_ShouldPersist() {
        PaymentEntity saved = paymentRepository.save(createPayment("TXN-2026-00001"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getInternalTransactionId()).isEqualTo("TXN-2026-00001");
    }

    @Test
    @DisplayName("findByBookingId() should find a payment by booking ID")
        // Busca un pago asociado a una reserva especifica
    void findByBookingId_ShouldFind() {
        paymentRepository.save(createPayment("TXN-2026-00002"));

        Optional<PaymentEntity> found = paymentRepository.findByBookingId(booking.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getAmountPaid()).isEqualByComparingTo("500000");
    }

    @Test
    @DisplayName("findByInternalTransactionId() should find a payment by transaction ID")
        // Busca un pago por su codigo de transaccion unico
    void findByInternalTransactionId_ShouldFind() {
        paymentRepository.save(createPayment("TXN-2026-99999"));

        Optional<PaymentEntity> found = paymentRepository.findByInternalTransactionId("TXN-2026-99999");

        assertThat(found).isPresent();
    }

    @Test
    @DisplayName("findByBookingId() should return empty when no payment exists")
        // Verifica que devuelve vacio si la reserva no tiene pago asociado
    void findByBookingId_WhenNotExists_ShouldReturnEmpty() {
        Optional<PaymentEntity> found = paymentRepository.findByBookingId(99999L);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findByPaymentDateBetween() should filter payments in date range")
        // Filtra pagos dentro de un rango de fechas (para reportes)
    void findByPaymentDateBetween_ShouldFilter() {
        paymentRepository.save(createPayment("TXN-RANGE-001"));

        List<PaymentEntity> result = paymentRepository.findByPaymentDateBetween(
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1)
        );

        assertThat(result).hasSize(1);
    }
}