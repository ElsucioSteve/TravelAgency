package com.travelagency.backend.services;

import com.travelagency.backend.entities.*;
import com.travelagency.backend.repositories.BookingRepository;
import com.travelagency.backend.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private TravelPackageService travelPackageService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DiscountConfigService discountConfigService;

    @InjectMocks
    private BookingService bookingService;

    private UserEntity user;
    private TravelPackageEntity travelPackage;
    private BookingEntity booking;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setId(1L);
        user.setEmail("test@test.com");
        user.setFullName("Test User");
        user.setAccountStatus("ACTIVE");

        travelPackage = new TravelPackageEntity();
        travelPackage.setId(1L);
        travelPackage.setName("Tour Test");
        travelPackage.setDestination("Chile");
        travelPackage.setStartDate(LocalDate.now().plusDays(30));
        travelPackage.setEndDate(LocalDate.now().plusDays(35));
        travelPackage.setBasePrice(new BigDecimal("500000"));
        travelPackage.setTotalSlots(20);
        travelPackage.setAvailableSlots(20);
        travelPackage.setPackageStatus("AVAILABLE");

        booking = new BookingEntity();
        booking.setId(1L);
        booking.setUser(user);
        booking.setTravelPackage(travelPackage);
        booking.setBookingDate(LocalDateTime.now());
        booking.setPassengerCount(2);
        booking.setGrossAmount(new BigDecimal("1000000"));
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setFinalAmount(new BigDecimal("1000000"));
        booking.setBookingStatus("PENDING");
        booking.setBookingCode("BK-2026-12345");
        booking.setPaymentExpirationDate(LocalDateTime.now().plusHours(24));
    }

    @Test
    @DisplayName("getAllBookings() should return all bookings")
        // Devuelve todas las reservas (admin)
    void getAllBookings_ShouldReturnAll() {
        when(bookingRepository.findAll()).thenReturn(List.of(booking));

        List<BookingEntity> result = bookingService.getAllBookings();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getBookingById() should return the booking when it exists")
        // Busca una reserva por ID exitosamente
    void getBookingById_WhenExists_ShouldReturn() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        BookingEntity result = bookingService.getBookingById(1L);

        assertThat(result.getBookingCode()).isEqualTo("BK-2026-12345");
    }

    @Test
    @DisplayName("getBookingById() should throw when booking does not exist")
        // Lanza excepcion si la reserva no existe
    void getBookingById_WhenNotExists_ShouldThrow() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getBookingById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("getBookingsByUserId() should return user bookings")
        // Devuelve las reservas de un usuario especifico
    void getBookingsByUserId_ShouldReturn() {
        when(bookingRepository.findByUserId(1L)).thenReturn(List.of(booking));

        List<BookingEntity> result = bookingService.getBookingsByUserId(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getBookingsByUserEmail() should return bookings when user exists")
        // Encuentra las reservas a traves del email del usuario
    void getBookingsByUserEmail_WhenUserExists_ShouldReturn() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(bookingRepository.findByUserId(1L)).thenReturn(List.of(booking));

        List<BookingEntity> result = bookingService.getBookingsByUserEmail("test@test.com");

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getBookingsByUserEmail() should return empty when user not exists")
        // Devuelve lista vacia si el usuario no existe
    void getBookingsByUserEmail_WhenUserNotExists_ShouldReturnEmpty() {
        when(userRepository.findByEmail("nope@test.com")).thenReturn(Optional.empty());

        List<BookingEntity> result = bookingService.getBookingsByUserEmail("nope@test.com");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("calculatePrice() without discounts should return gross = final")
        // Sin descuentos aplicables, el monto final = bruto
        // Con 2 pasajeros (< 4) el GROUP no aplica; con 0 confirmados FREQUENT tampoco;
        // pero igual se consulta countByUserIdAndBookingStatus para MULTI_PACKAGE
    void calculatePrice_WithoutDiscounts_ShouldReturnGross() {
        when(travelPackageService.getPackageById(1L)).thenReturn(travelPackage);
        // El servicio consulta CONFIRMED (para FREQUENT) y PENDING (para MULTI_PACKAGE)
        when(bookingRepository.countByUserIdAndBookingStatus(1L, "CONFIRMED")).thenReturn(0L);
        when(bookingRepository.countByUserIdAndBookingStatus(1L, "PENDING")).thenReturn(0L);

        BookingService.BookingPriceBreakdown result = bookingService.calculatePrice(1L, 1L, 2);

        assertThat(result.grossAmount).isEqualByComparingTo("1000000");
        assertThat(result.discountAmount).isEqualByComparingTo("0");
        assertThat(result.finalAmount).isEqualByComparingTo("1000000");
    }

    @Test
    @DisplayName("calculatePrice() with 4+ passengers should apply GROUP discount")
        // Con 4+ pasajeros se aplica el descuento de grupo
    void calculatePrice_WithGroup_ShouldApplyGroupDiscount() {
        DiscountConfigEntity groupDiscount = createDiscount("Group 10%", "GROUP", "PERCENTAGE",
                new BigDecimal("10"), true);

        when(travelPackageService.getPackageById(1L)).thenReturn(travelPackage);
        when(bookingRepository.countByUserIdAndBookingStatus(1L, "CONFIRMED")).thenReturn(0L);
        when(bookingRepository.countByUserIdAndBookingStatus(1L, "PENDING")).thenReturn(0L);
        when(discountConfigService.getActiveDiscountsByCriteria("GROUP"))
                .thenReturn(List.of(groupDiscount));

        // 4 pasajeros: 4 * 500000 = 2,000,000 bruto; 10% = 200,000 descuento
        BookingService.BookingPriceBreakdown result = bookingService.calculatePrice(1L, 1L, 4);

        assertThat(result.grossAmount).isEqualByComparingTo("2000000");
        assertThat(result.discountAmount).isEqualByComparingTo("200000");
        assertThat(result.finalAmount).isEqualByComparingTo("1800000");
    }

    @Test
    @DisplayName("calculatePrice() should respect the 20% global cap")
        // El tope global del 20% se aplica si la suma de descuentos lo supera
    void calculatePrice_WithExcessiveDiscount_ShouldCapAt20Percent() {
        DiscountConfigEntity bigDiscount = createDiscount("Mega 50%", "GROUP", "PERCENTAGE",
                new BigDecimal("50"), true);

        when(travelPackageService.getPackageById(1L)).thenReturn(travelPackage);
        when(bookingRepository.countByUserIdAndBookingStatus(1L, "CONFIRMED")).thenReturn(0L);
        when(bookingRepository.countByUserIdAndBookingStatus(1L, "PENDING")).thenReturn(0L);
        when(discountConfigService.getActiveDiscountsByCriteria("GROUP"))
                .thenReturn(List.of(bigDiscount));

        // 4 pax * 500000 = 2,000,000 bruto. 50% = 1,000,000 pero el tope es 20% = 400,000
        BookingService.BookingPriceBreakdown result = bookingService.calculatePrice(1L, 1L, 4);

        assertThat(result.discountAmount).isEqualByComparingTo("400000");
        assertThat(result.finalAmount).isEqualByComparingTo("1600000");
    }

    @Test
    @DisplayName("createBooking() should fail when user is INACTIVE")
        // Un usuario INACTIVE no puede reservar
    void createBooking_WhenUserInactive_ShouldThrow() {
        user.setAccountStatus("INACTIVE");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> bookingService.createBooking(1L, 1L, 2, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("INACTIVE");
    }

    @Test
    @DisplayName("createBooking() should fail when package is not AVAILABLE")
        // No se puede reservar un paquete SOLD_OUT, EXPIRED o CANCELED
    void createBooking_WhenPackageNotAvailable_ShouldThrow() {
        travelPackage.setPackageStatus("SOLD_OUT");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(travelPackageService.getPackageById(1L)).thenReturn(travelPackage);

        assertThatThrownBy(() -> bookingService.createBooking(1L, 1L, 2, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not available");
    }

    @Test
    @DisplayName("createBooking() should fail when not enough slots")
        // No se puede reservar mas pasajeros que cupos disponibles
    void createBooking_WhenNotEnoughSlots_ShouldThrow() {
        travelPackage.setAvailableSlots(1);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(travelPackageService.getPackageById(1L)).thenReturn(travelPackage);

        assertThatThrownBy(() -> bookingService.createBooking(1L, 1L, 5, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Not enough");
    }

    @Test
    @DisplayName("createBooking() should fail with zero or negative passenger count")
        // Cantidad de pasajeros debe ser positiva
    void createBooking_WithZeroPassengers_ShouldThrow() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(travelPackageService.getPackageById(1L)).thenReturn(travelPackage);

        assertThatThrownBy(() -> bookingService.createBooking(1L, 1L, 0, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    @DisplayName("createBooking() happy path should save booking and decrement slots")
        // Caso feliz: crea la reserva, descuenta cupos, asigna codigo
    void createBooking_HappyPath_ShouldSucceed() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(travelPackageService.getPackageById(1L)).thenReturn(travelPackage);
        when(bookingRepository.countByUserIdAndBookingStatus(1L, "CONFIRMED")).thenReturn(0L);
        when(bookingRepository.countByUserIdAndBookingStatus(1L, "PENDING")).thenReturn(0L);
        when(bookingRepository.save(any(BookingEntity.class))).thenAnswer(inv -> {
            BookingEntity b = inv.getArgument(0);
            b.setId(1L);
            return b;
        });

        BookingEntity result = bookingService.createBooking(1L, 1L, 2, null);

        assertThat(result).isNotNull();
        assertThat(result.getBookingStatus()).isEqualTo("PENDING");
        verify(travelPackageService).decrementAvailableSlots(1L, 2);
        verify(bookingRepository, org.mockito.Mockito.atLeastOnce()).save(any(BookingEntity.class));
    }

    @Test
    @DisplayName("cancelBooking() should free slots and mark as CANCELED")
        // Cancelar libera cupos y cambia estado a CANCELED
    void cancelBooking_ShouldFreeSlotsAndCancel() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(BookingEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        bookingService.cancelBooking(1L);

        assertThat(booking.getBookingStatus()).isEqualTo("CANCELED");
        assertThat(travelPackage.getAvailableSlots()).isEqualTo(22);  // 20 + 2 devueltos
    }

    @Test
    @DisplayName("cancelBooking() of CONFIRMED booking should throw")
        // Una reserva pagada no se puede cancelar (contactar admin)
    void cancelBooking_WhenConfirmed_ShouldThrow() {
        booking.setBookingStatus("CONFIRMED");
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot cancel a confirmed");
    }

    @Test
    @DisplayName("cancelBooking() of already CANCELED should throw")
        // Una reserva ya cancelada no se puede volver a cancelar
    void cancelBooking_WhenAlreadyCanceled_ShouldThrow() {
        booking.setBookingStatus("CANCELED");
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already canceled");
    }

    @Test
    @DisplayName("confirmBookingAfterPayment() should change PENDING to CONFIRMED")
        // Confirma la reserva tras pago exitoso
    void confirmBookingAfterPayment_ShouldConfirm() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(BookingEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingEntity result = bookingService.confirmBookingAfterPayment(1L);

        assertThat(result.getBookingStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("confirmBookingAfterPayment() should fail if not PENDING")
        // Solo reservas PENDING pueden confirmarse
    void confirmBookingAfterPayment_NotPending_ShouldThrow() {
        booking.setBookingStatus("CONFIRMED");
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.confirmBookingAfterPayment(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Only PENDING");
    }

    @Test
    @DisplayName("expirePendingBookings() should expire bookings past deadline")
        // Expira reservas PENDING cuya fecha de pago ya paso
    void expirePendingBookings_ShouldExpire() {
        booking.setPaymentExpirationDate(LocalDateTime.now().minusHours(1));

        when(bookingRepository.findByBookingStatus("PENDING")).thenReturn(List.of(booking));
        when(bookingRepository.save(any(BookingEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        int count = bookingService.expirePendingBookings();

        assertThat(count).isEqualTo(1);
        assertThat(booking.getBookingStatus()).isEqualTo("EXPIRED");
        assertThat(travelPackage.getAvailableSlots()).isEqualTo(22);  // cupos devueltos
    }

    @Test
    @DisplayName("expirePendingBookings() should NOT expire bookings not yet past deadline")
        // No expira reservas cuyo plazo aun no termina
    void expirePendingBookings_NotPastDeadline_ShouldNotExpire() {
        booking.setPaymentExpirationDate(LocalDateTime.now().plusHours(5));

        when(bookingRepository.findByBookingStatus("PENDING")).thenReturn(List.of(booking));

        int count = bookingService.expirePendingBookings();

        assertThat(count).isEqualTo(0);
        assertThat(booking.getBookingStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("cancelPendingBookingsForUser() should cancel all PENDING and free slots")
        // Al desactivar un usuario, todas sus PENDING pasan a CANCELED
    void cancelPendingBookingsForUser_ShouldCancelAll() {
        when(bookingRepository.findByUserIdAndBookingStatus(1L, "PENDING"))
                .thenReturn(List.of(booking));
        when(bookingRepository.save(any(BookingEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        int count = bookingService.cancelPendingBookingsForUser(1L);

        assertThat(count).isEqualTo(1);
        assertThat(booking.getBookingStatus()).isEqualTo("CANCELED");
    }

    @Test
    @DisplayName("calculatePrice() with FREQUENT client should apply discount")
        // Cliente con 3+ reservas confirmadas obtiene descuento FREQUENT
    void calculatePrice_WithFrequentClient_ShouldApplyDiscount() {
        DiscountConfigEntity frequentDiscount = createDiscount("Frequent 5%", "FREQUENT", "PERCENTAGE",
                new BigDecimal("5"), true);

        when(travelPackageService.getPackageById(1L)).thenReturn(travelPackage);
        when(bookingRepository.countByUserIdAndBookingStatus(1L, "CONFIRMED")).thenReturn(5L);
        when(bookingRepository.countByUserIdAndBookingStatus(1L, "PENDING")).thenReturn(0L);
        when(discountConfigService.getActiveDiscountsByCriteria("FREQUENT"))
                .thenReturn(List.of(frequentDiscount));
        when(discountConfigService.getActiveDiscountsByCriteria("MULTI_PACKAGE"))
                .thenReturn(List.of());

        // 2 pasajeros: no aplica GROUP. Solo FREQUENT (5% de 1,000,000 = 50,000)
        BookingService.BookingPriceBreakdown result = bookingService.calculatePrice(1L, 1L, 2);

        assertThat(result.discountAmount).isEqualByComparingTo("50000");
    }

    @Test
    @DisplayName("calculatePrice() with FIXED_AMOUNT discount should apply correctly")
        // Verifica que descuentos de monto fijo se aplican (no como porcentaje)
    void calculatePrice_WithFixedAmountDiscount_ShouldApply() {
        DiscountConfigEntity fixedDiscount = createDiscount("Fixed 100k", "GROUP", "FIXED_AMOUNT",
                new BigDecimal("100000"), true);

        when(travelPackageService.getPackageById(1L)).thenReturn(travelPackage);
        when(bookingRepository.countByUserIdAndBookingStatus(1L, "CONFIRMED")).thenReturn(0L);
        when(bookingRepository.countByUserIdAndBookingStatus(1L, "PENDING")).thenReturn(0L);
        when(discountConfigService.getActiveDiscountsByCriteria("GROUP"))
                .thenReturn(List.of(fixedDiscount));

        BookingService.BookingPriceBreakdown result = bookingService.calculatePrice(1L, 1L, 4);

        // Descuento fijo de 100,000
        assertThat(result.discountAmount).isEqualByComparingTo("100000");
    }

    @Test
    @DisplayName("calculatePrice() should respect maxLimit on individual discount")
        // El tope individual de un descuento se aplica antes del tope global
    void calculatePrice_WithMaxLimit_ShouldCapIndividual() {
        DiscountConfigEntity capped = createDiscount("Capped", "GROUP", "PERCENTAGE",
                new BigDecimal("15"), true);
        capped.setMaxLimit(new BigDecimal("50000"));  // tope: 50,000

        when(travelPackageService.getPackageById(1L)).thenReturn(travelPackage);
        when(bookingRepository.countByUserIdAndBookingStatus(1L, "CONFIRMED")).thenReturn(0L);
        when(bookingRepository.countByUserIdAndBookingStatus(1L, "PENDING")).thenReturn(0L);
        when(discountConfigService.getActiveDiscountsByCriteria("GROUP"))
                .thenReturn(List.of(capped));

        // 4 pax: 2,000,000 * 15% = 300,000, pero el tope individual es 50,000
        BookingService.BookingPriceBreakdown result = bookingService.calculatePrice(1L, 1L, 4);

        assertThat(result.discountAmount).isEqualByComparingTo("50000");
    }

    @Test
    @DisplayName("calculatePrice() with non-stackable discount should stop applying further")
        // Un descuento no-apilable bloquea los siguientes descuentos del mismo criterio
    void calculatePrice_WithNonStackable_ShouldStop() {
        DiscountConfigEntity nonStackable = createDiscount("Exclusive 10%", "GROUP", "PERCENTAGE",
                new BigDecimal("10"), false);  // no apilable

        when(travelPackageService.getPackageById(1L)).thenReturn(travelPackage);
        when(bookingRepository.countByUserIdAndBookingStatus(1L, "CONFIRMED")).thenReturn(0L);
        when(bookingRepository.countByUserIdAndBookingStatus(1L, "PENDING")).thenReturn(0L);
        when(discountConfigService.getActiveDiscountsByCriteria("GROUP"))
                .thenReturn(List.of(nonStackable));

        BookingService.BookingPriceBreakdown result = bookingService.calculatePrice(1L, 1L, 4);

        // Aplica solo el descuento no-apilable
        assertThat(result.discountAmount).isEqualByComparingTo("200000");
    }

    @Test
    @DisplayName("calculatePrice() with PROMO selected should apply on top")
        // PROMO seleccionada se suma a los descuentos automaticos si es apilable
    void calculatePrice_WithStackablePromo_ShouldAddToOthers() {
        DiscountConfigEntity promo = createDiscount("Black Friday", "PROMO", "PERCENTAGE",
                new BigDecimal("5"), true);
        promo.setId(99L);
        promo.setStartDate(LocalDateTime.now().minusDays(1));
        promo.setEndDate(LocalDateTime.now().plusDays(7));

        when(travelPackageService.getPackageById(1L)).thenReturn(travelPackage);
        when(bookingRepository.countByUserIdAndBookingStatus(1L, "CONFIRMED")).thenReturn(0L);
        when(bookingRepository.countByUserIdAndBookingStatus(1L, "PENDING")).thenReturn(0L);
        when(discountConfigService.getById(99L)).thenReturn(promo);

        // Sin GROUP/FREQUENT/MULTI, solo el PROMO. 2 pax = 1,000,000 * 5% = 50,000
        BookingService.BookingPriceBreakdown result = bookingService.calculatePrice(1L, 1L, 2, 99L);

        assertThat(result.discountAmount).isEqualByComparingTo("50000");
    }

    @Test
    @DisplayName("calculatePrice() with non-stackable PROMO should skip automatic discounts")
        // PROMO no-apilable: SOLO se aplica el PROMO, sin descuentos automaticos
    void calculatePrice_WithNonStackablePromo_ShouldSkipAutomatic() {
        DiscountConfigEntity promo = createDiscount("Exclusive Promo", "PROMO", "PERCENTAGE",
                new BigDecimal("8"), false);  // no apilable
        promo.setId(100L);
        promo.setStartDate(LocalDateTime.now().minusDays(1));
        promo.setEndDate(LocalDateTime.now().plusDays(7));

        when(travelPackageService.getPackageById(1L)).thenReturn(travelPackage);
        when(discountConfigService.getById(100L)).thenReturn(promo);

        // Aunque hay 4 pasajeros, no se aplica GROUP porque la PROMO es exclusiva
        BookingService.BookingPriceBreakdown result = bookingService.calculatePrice(1L, 1L, 4, 100L);

        // Solo PROMO: 2,000,000 * 8% = 160,000
        assertThat(result.discountAmount).isEqualByComparingTo("160000");
    }

    @Test
    @DisplayName("calculatePrice() with non-applicable PROMO should ignore it")
        // PROMO fuera de vigencia debe ignorarse silenciosamente
    void calculatePrice_WithExpiredPromo_ShouldIgnore() {
        DiscountConfigEntity expiredPromo = createDiscount("Old Promo", "PROMO", "PERCENTAGE",
                new BigDecimal("10"), true);
        expiredPromo.setId(101L);
        expiredPromo.setStartDate(LocalDateTime.now().minusDays(30));
        expiredPromo.setEndDate(LocalDateTime.now().minusDays(15));  // ya vencio

        when(travelPackageService.getPackageById(1L)).thenReturn(travelPackage);
        when(bookingRepository.countByUserIdAndBookingStatus(1L, "CONFIRMED")).thenReturn(0L);
        when(bookingRepository.countByUserIdAndBookingStatus(1L, "PENDING")).thenReturn(0L);
        when(discountConfigService.getById(101L)).thenReturn(expiredPromo);

        BookingService.BookingPriceBreakdown result = bookingService.calculatePrice(1L, 1L, 2, 101L);

        // Sin descuentos aplicables
        assertThat(result.discountAmount).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("calculatePrice() with PROMO ID that throws should ignore promo")
        // Si discountConfigService.getById lanza excepcion, simplemente se ignora la promo
    void calculatePrice_WithPromoIdThatThrows_ShouldIgnore() {
        when(travelPackageService.getPackageById(1L)).thenReturn(travelPackage);
        when(bookingRepository.countByUserIdAndBookingStatus(1L, "CONFIRMED")).thenReturn(0L);
        when(bookingRepository.countByUserIdAndBookingStatus(1L, "PENDING")).thenReturn(0L);
        when(discountConfigService.getById(999L)).thenThrow(new RuntimeException("Not found"));

        BookingService.BookingPriceBreakdown result = bookingService.calculatePrice(1L, 1L, 2, 999L);

        assertThat(result.discountAmount).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("createBooking() should fail when passengers list size doesn't match passengerCount")
        // La lista de pasajeros debe coincidir con el conteo
    void createBooking_WithMismatchedPassengerList_ShouldThrow() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(travelPackageService.getPackageById(1L)).thenReturn(travelPackage);

        BookingPassengerEntity p1 = new BookingPassengerEntity();
        p1.setFullName("Passenger 1");

        assertThatThrownBy(() -> bookingService.createBooking(1L, 1L, 3, List.of(p1)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    @DisplayName("createBooking() with valid passengers list should attach them")
        // Lista de pasajeros se asigna correctamente
    void createBooking_WithPassengers_ShouldAttach() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(travelPackageService.getPackageById(1L)).thenReturn(travelPackage);
        when(bookingRepository.countByUserIdAndBookingStatus(1L, "CONFIRMED")).thenReturn(0L);
        when(bookingRepository.countByUserIdAndBookingStatus(1L, "PENDING")).thenReturn(0L);
        when(bookingRepository.save(any(BookingEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingPassengerEntity p1 = new BookingPassengerEntity();
        p1.setFullName("Passenger 1");
        BookingPassengerEntity p2 = new BookingPassengerEntity();
        p2.setFullName("Passenger 2");

        BookingEntity result = bookingService.createBooking(1L, 1L, 2, List.of(p1, p2));

        assertThat(result.getPassengers()).hasSize(2);
    }

    @Test
    @DisplayName("cancelBooking() should revert SOLD_OUT to AVAILABLE if freeing slots")
        // Si el paquete estaba SOLD_OUT y se liberan cupos, vuelve a AVAILABLE
    void cancelBooking_WhenPackageSoldOut_ShouldRevertToAvailable() {
        travelPackage.setAvailableSlots(0);
        travelPackage.setPackageStatus("SOLD_OUT");

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(BookingEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        bookingService.cancelBooking(1L);

        assertThat(travelPackage.getPackageStatus()).isEqualTo("AVAILABLE");
        assertThat(travelPackage.getAvailableSlots()).isEqualTo(2);
    }

    @Test
    @DisplayName("cancelBooking() of EXPIRED booking should throw")
        // Una reserva expirada no se puede cancelar
    void cancelBooking_WhenExpired_ShouldThrow() {
        booking.setBookingStatus("EXPIRED");
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already canceled or expired");
    }

    @Test
    @DisplayName("expirePendingBookings() should revert SOLD_OUT package to AVAILABLE")
        // Si expira reservas y el paquete estaba SOLD_OUT, vuelve a AVAILABLE
    void expirePendingBookings_WhenPackageSoldOut_ShouldRevertToAvailable() {
        travelPackage.setAvailableSlots(0);
        travelPackage.setPackageStatus("SOLD_OUT");
        booking.setPaymentExpirationDate(LocalDateTime.now().minusHours(1));

        when(bookingRepository.findByBookingStatus("PENDING")).thenReturn(List.of(booking));
        when(bookingRepository.save(any(BookingEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        bookingService.expirePendingBookings();

        assertThat(travelPackage.getPackageStatus()).isEqualTo("AVAILABLE");
    }

    @Test
    @DisplayName("cancelPendingBookingsForUser() should revert SOLD_OUT package to AVAILABLE")
        // Al cancelar pendientes por desactivar usuario, paquete SOLD_OUT vuelve a AVAILABLE
    void cancelPendingBookingsForUser_WhenPackageSoldOut_ShouldRevertToAvailable() {
        travelPackage.setAvailableSlots(0);
        travelPackage.setPackageStatus("SOLD_OUT");

        when(bookingRepository.findByUserIdAndBookingStatus(1L, "PENDING"))
                .thenReturn(List.of(booking));
        when(bookingRepository.save(any(BookingEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        bookingService.cancelPendingBookingsForUser(1L);

        assertThat(travelPackage.getPackageStatus()).isEqualTo("AVAILABLE");
    }

    // Helper para crear descuentos de prueba
    private DiscountConfigEntity createDiscount(String name, String criteria, String valueType,
                                                BigDecimal value, boolean stackable) {
        DiscountConfigEntity d = new DiscountConfigEntity();
        d.setId(1L);
        d.setName(name);
        d.setApplicationCriteria(criteria);
        d.setValueType(valueType);
        d.setDiscountValue(value);
        d.setDiscountStatus("ACTIVE");
        d.setIsStackable(stackable);
        return d;
    }
}