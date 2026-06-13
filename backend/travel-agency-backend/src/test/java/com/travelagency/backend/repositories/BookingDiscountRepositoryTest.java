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
class BookingDiscountRepositoryTest {

    @Autowired
    private BookingDiscountRepository bookingDiscountRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private DiscountConfigRepository discountConfigRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TravelPackageRepository travelPackageRepository;

    private BookingEntity booking;
    private DiscountConfigEntity discount;

    @BeforeEach
    void setUp() {
        bookingDiscountRepository.deleteAll();
        bookingRepository.deleteAll();
        discountConfigRepository.deleteAll();
        userRepository.deleteAll();
        travelPackageRepository.deleteAll();

        UserEntity user = new UserEntity();
        user.setEmail("bd@test.com");
        user.setFullName("BD User");
        user.setKeycloakId("kc-bd");
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
        booking.setPassengerCount(4);
        booking.setGrossAmount(new BigDecimal("2000000"));
        booking.setDiscountAmount(new BigDecimal("200000"));
        booking.setFinalAmount(new BigDecimal("1800000"));
        booking.setBookingStatus("CONFIRMED");
        booking.setBookingCode("BK-BD-001");
        booking = bookingRepository.save(booking);

        discount = new DiscountConfigEntity();
        discount.setName("Group Discount");
        discount.setApplicationCriteria("GROUP");
        discount.setValueType("PERCENTAGE");
        discount.setDiscountValue(new BigDecimal("10"));
        discount.setDiscountStatus("ACTIVE");
        discount.setIsStackable(true);
        discount = discountConfigRepository.save(discount);
    }

    @Test
    @DisplayName("save() should persist a booking-discount with composite key")
        // Guarda un descuento aplicado a una reserva (clave compuesta)
    void save_ShouldPersistWithCompositeKey() {
        BookingDiscountEntity bd = new BookingDiscountEntity();
        BookingDiscountEntity.BookingDiscountId id =
                new BookingDiscountEntity.BookingDiscountId(booking.getId(), discount.getId());
        bd.setId(id);
        bd.setBooking(booking);
        bd.setDiscount(discount);
        bd.setAppliedAmount(new BigDecimal("200000"));

        BookingDiscountEntity saved = bookingDiscountRepository.save(bd);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAppliedAmount()).isEqualByComparingTo("200000");
    }

    @Test
    @DisplayName("findAll() should return all booking-discounts")
        // Lista todos los descuentos aplicados a reservas
    void findAll_ShouldReturnAll() {
        BookingDiscountEntity bd = new BookingDiscountEntity();
        BookingDiscountEntity.BookingDiscountId id =
                new BookingDiscountEntity.BookingDiscountId(booking.getId(), discount.getId());
        bd.setId(id);
        bd.setBooking(booking);
        bd.setDiscount(discount);
        bd.setAppliedAmount(new BigDecimal("200000"));
        bookingDiscountRepository.save(bd);

        List<BookingDiscountEntity> all = bookingDiscountRepository.findAll();
        assertThat(all).hasSize(1);
    }
}