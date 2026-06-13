package com.travelagency.backend.services;

import com.travelagency.backend.entities.*;
import com.travelagency.backend.repositories.BookingRepository;
import com.travelagency.backend.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TravelPackageService travelPackageService;
    private final UserRepository userRepository;
    private final DiscountConfigService discountConfigService;

    private static final int GROUP_DISCOUNT_THRESHOLD = 4;
    private static final int FREQUENT_CLIENT_THRESHOLD = 3;
    private static final BigDecimal MAX_DISCOUNT_PERCENT = new BigDecimal("20");
    private static final int PAYMENT_EXPIRATION_HOURS = 24;

    public BookingService(BookingRepository bookingRepository,
                          TravelPackageService travelPackageService,
                          UserRepository userRepository,
                          DiscountConfigService discountConfigService) {
        this.bookingRepository = bookingRepository;
        this.travelPackageService = travelPackageService;
        this.userRepository = userRepository;
        this.discountConfigService = discountConfigService;
    }

    // ----- LECTURAS -----

    public List<BookingEntity> getAllBookings() {
        return bookingRepository.findAll();
    }

    public BookingEntity getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + id));
    }

    public List<BookingEntity> getBookingsByUserId(Long userId) {
        return bookingRepository.findByUserId(userId);
    }

    public List<BookingEntity> getBookingsByUserEmail(String email) {
        Optional<UserEntity> user = userRepository.findByEmail(email);
        if (user.isEmpty()) return List.of();
        return bookingRepository.findByUserId(user.get().getId());
    }

    // ----- CALCULO DE PRECIO (ahora dinamico desde la BD) -----

    public BookingPriceBreakdown calculatePrice(Long userId, Long packageId, int passengerCount) {
        return calculatePrice(userId, packageId, passengerCount, null);
    }

    public BookingPriceBreakdown calculatePrice(Long userId, Long packageId, int passengerCount, Long promoDiscountId) {
        TravelPackageEntity pkg = travelPackageService.getPackageById(packageId);

        // 1. Monto bruto
        BigDecimal gross = pkg.getBasePrice().multiply(BigDecimal.valueOf(passengerCount));

        // 2. Si hay un PROMO elegido, validarlo
        DiscountConfigEntity selectedPromo = null;
        boolean promoIsNonStackable = false;

        if (promoDiscountId != null) {
            try {
                selectedPromo = discountConfigService.getById(promoDiscountId);
                if (selectedPromo != null && isPromoApplicable(selectedPromo)) {
                    promoIsNonStackable = !Boolean.TRUE.equals(selectedPromo.getIsStackable());
                } else {
                    selectedPromo = null;
                }
            } catch (Exception e) {
                selectedPromo = null;
            }
        }

        // 3. Determinar criterios aplicables
        List<String> applicableCriteria = new ArrayList<>();

        if (!promoIsNonStackable) {
            if (passengerCount >= GROUP_DISCOUNT_THRESHOLD) {
                applicableCriteria.add("GROUP");
            }

            long pastConfirmed = bookingRepository.countByUserIdAndBookingStatus(userId, "CONFIRMED");
            if (pastConfirmed >= FREQUENT_CLIENT_THRESHOLD) {
                applicableCriteria.add("FREQUENT");
            }

            long pendingCount = bookingRepository.countByUserIdAndBookingStatus(userId, "PENDING");
            if (pastConfirmed >= 1 || pendingCount >= 1) {
                applicableCriteria.add("MULTI_PACKAGE");
            }
        }

        // 4. Buscar descuentos activos
        List<AppliedDiscount> appliedDiscounts = new ArrayList<>();
        BigDecimal totalDiscount = BigDecimal.ZERO;
        boolean nonStackableApplied = false;

        for (String criteria : applicableCriteria) {
            List<DiscountConfigEntity> available = discountConfigService.getActiveDiscountsByCriteria(criteria);

            for (DiscountConfigEntity discount : available) {
                if (nonStackableApplied) break;

                BigDecimal discountAmount = calculateDiscountAmount(discount, gross);
                appliedDiscounts.add(new AppliedDiscount(discount, discountAmount));
                totalDiscount = totalDiscount.add(discountAmount);

                if (!Boolean.TRUE.equals(discount.getIsStackable())) {
                    nonStackableApplied = true;
                }
            }
        }

        // 5. Agregar el PROMO al final si fue seleccionado
        if (selectedPromo != null) {
            BigDecimal promoAmount = calculateDiscountAmount(selectedPromo, gross);
            appliedDiscounts.add(new AppliedDiscount(selectedPromo, promoAmount));
            totalDiscount = totalDiscount.add(promoAmount);
        }

        // 6. Aplicar tope global de descuento (20%)
        BigDecimal maxAllowed = gross.multiply(MAX_DISCOUNT_PERCENT)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        if (totalDiscount.compareTo(maxAllowed) > 0) {
            BigDecimal ratio = maxAllowed.divide(totalDiscount, 4, RoundingMode.HALF_UP);
            for (AppliedDiscount ad : appliedDiscounts) {
                ad.amount = ad.amount.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
            }
            totalDiscount = maxAllowed;
        }

        // 7. Calcular final
        BigDecimal finalAmount = gross.subtract(totalDiscount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        // 8. Armar descripcion
        StringBuilder reasons = new StringBuilder();
        for (AppliedDiscount ad : appliedDiscounts) {
            reasons.append(ad.discount.getName())
                    .append(" ($").append(ad.amount).append("); ");
        }

        BigDecimal discountPercent = BigDecimal.ZERO;
        if (gross.compareTo(BigDecimal.ZERO) > 0) {
            discountPercent = totalDiscount.multiply(BigDecimal.valueOf(100))
                    .divide(gross, 2, RoundingMode.HALF_UP);
        }

        return new BookingPriceBreakdown(
                gross, totalDiscount, finalAmount, discountPercent,
                reasons.toString().trim(), appliedDiscounts
        );
    }

    private boolean isPromoApplicable(DiscountConfigEntity promo) {
        if (!"PROMO".equals(promo.getApplicationCriteria())) return false;
        if (!"ACTIVE".equals(promo.getDiscountStatus())) return false;

        LocalDateTime now = LocalDateTime.now();
        if (promo.getStartDate() != null && promo.getStartDate().isAfter(now)) return false;
        if (promo.getEndDate() != null && promo.getEndDate().isBefore(now)) return false;

        return true;
    }

    private BigDecimal calculateDiscountAmount(DiscountConfigEntity discount, BigDecimal gross) {
        BigDecimal amount;

        if ("PERCENTAGE".equals(discount.getValueType())) {
            amount = gross.multiply(discount.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            amount = discount.getDiscountValue();
        }

        if (discount.getMaxLimit() != null && amount.compareTo(discount.getMaxLimit()) > 0) {
            amount = discount.getMaxLimit();
        }

        return amount;
    }

    // ----- CREAR RESERVA -----

    @Transactional
    public BookingEntity createBooking(Long userId, Long packageId, int passengerCount,
                                       List<BookingPassengerEntity> passengers) {
        return createBooking(userId, packageId, passengerCount, passengers, null);
    }

    @Transactional
    public BookingEntity createBooking(Long userId, Long packageId, int passengerCount,
                                       List<BookingPassengerEntity> passengers,
                                       Long promoDiscountId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // VALIDACION: el usuario debe estar ACTIVE para reservar
        if (!"ACTIVE".equals(user.getAccountStatus())) {
            throw new RuntimeException("Your account is " + user.getAccountStatus() +
                    ". You cannot make reservations.");
        }

        TravelPackageEntity pkg = travelPackageService.getPackageById(packageId);

        if (passengerCount <= 0) {
            throw new RuntimeException("Passenger count must be greater than zero");
        }

        if (!"AVAILABLE".equals(pkg.getPackageStatus())) {
            throw new RuntimeException("Package is not available for booking. Current status: " + pkg.getPackageStatus());
        }

        if (pkg.getAvailableSlots() < passengerCount) {
            throw new RuntimeException("Not enough available slots. Requested: " + passengerCount + ", available: " + pkg.getAvailableSlots());
        }

        if (passengers != null && !passengers.isEmpty() && passengers.size() != passengerCount) {
            throw new RuntimeException("Passenger list size (" + passengers.size() + ") does not match passengerCount (" + passengerCount + ")");
        }

        BookingPriceBreakdown breakdown = calculatePrice(userId, packageId, passengerCount, promoDiscountId);

        travelPackageService.decrementAvailableSlots(packageId, passengerCount);

        BookingEntity booking = new BookingEntity();
        booking.setUser(user);
        booking.setTravelPackage(pkg);
        booking.setBookingDate(LocalDateTime.now());
        booking.setPassengerCount(passengerCount);
        booking.setGrossAmount(breakdown.grossAmount);
        booking.setDiscountAmount(breakdown.discountAmount);
        booking.setFinalAmount(breakdown.finalAmount);
        booking.setBookingStatus("PENDING");
        booking.setPaymentExpirationDate(LocalDateTime.now().plusHours(PAYMENT_EXPIRATION_HOURS));
        booking.setBookingCode(generateBookingCode());

        if (passengers != null && !passengers.isEmpty()) {
            for (BookingPassengerEntity p : passengers) {
                p.setBooking(booking);
                booking.getPassengers().add(p);
            }
        }

        BookingEntity savedBooking = bookingRepository.save(booking);

        for (AppliedDiscount ad : breakdown.appliedDiscountsList) {
            BookingDiscountEntity bd = new BookingDiscountEntity();
            BookingDiscountEntity.BookingDiscountId bdId = new BookingDiscountEntity.BookingDiscountId(
                    savedBooking.getId(), ad.discount.getId()
            );
            bd.setId(bdId);
            bd.setBooking(savedBooking);
            bd.setDiscount(ad.discount);
            bd.setAppliedAmount(ad.amount);
            savedBooking.getAppliedDiscounts().add(bd);
        }

        return bookingRepository.save(savedBooking);
    }

    // ----- CANCELAR -----

    @Transactional
    public void cancelBooking(Long bookingId) {
        BookingEntity booking = getBookingById(bookingId);

        if ("CANCELED".equals(booking.getBookingStatus()) || "EXPIRED".equals(booking.getBookingStatus())) {
            throw new RuntimeException("Booking is already canceled or expired");
        }

        if ("CONFIRMED".equals(booking.getBookingStatus())) {
            throw new RuntimeException("Cannot cancel a confirmed (paid) booking. Contact admin for refund.");
        }

        TravelPackageEntity pkg = booking.getTravelPackage();
        pkg.setAvailableSlots(pkg.getAvailableSlots() + booking.getPassengerCount());

        if ("SOLD_OUT".equals(pkg.getPackageStatus()) && pkg.getAvailableSlots() > 0) {
            pkg.setPackageStatus("AVAILABLE");
        }

        booking.setBookingStatus("CANCELED");
        bookingRepository.save(booking);
    }

    public BookingEntity confirmBookingAfterPayment(Long bookingId) {
        BookingEntity booking = getBookingById(bookingId);

        if (!"PENDING".equals(booking.getBookingStatus())) {
            throw new RuntimeException("Only PENDING bookings can be confirmed. Current status: " + booking.getBookingStatus());
        }

        booking.setBookingStatus("CONFIRMED");
        return bookingRepository.save(booking);
    }

    private String generateBookingCode() {
        long timestamp = System.currentTimeMillis() % 100000;
        return String.format("BK-%d-%05d", LocalDateTime.now().getYear(), timestamp);
    }

    // ----- EXPIRACION AUTOMATICA DE RESERVAS PENDING -----

    @Transactional
    public int expirePendingBookings() {
        LocalDateTime now = LocalDateTime.now();

        List<BookingEntity> expiredBookings = bookingRepository.findByBookingStatus("PENDING")
                .stream()
                .filter(b -> b.getPaymentExpirationDate() != null
                        && b.getPaymentExpirationDate().isBefore(now))
                .toList();

        int count = 0;
        for (BookingEntity booking : expiredBookings) {
            booking.setBookingStatus("EXPIRED");

            TravelPackageEntity pkg = booking.getTravelPackage();
            pkg.setAvailableSlots(pkg.getAvailableSlots() + booking.getPassengerCount());

            if ("SOLD_OUT".equals(pkg.getPackageStatus()) && pkg.getAvailableSlots() > 0) {
                pkg.setPackageStatus("AVAILABLE");
            }

            bookingRepository.save(booking);
            count++;
        }

        return count;
    }

    // ----- CANCELAR PENDIENTES POR DESACTIVACION DE USUARIO -----
    // Usado cuando un admin desactiva una cuenta.
    // Las CONFIRMED no se tocan (ya pagaron, son sagradas).

    @Transactional
    public int cancelPendingBookingsForUser(Long userId) {
        List<BookingEntity> pending = bookingRepository.findByUserIdAndBookingStatus(userId, "PENDING");

        int count = 0;
        for (BookingEntity booking : pending) {
            // Devolver cupos al paquete
            TravelPackageEntity pkg = booking.getTravelPackage();
            pkg.setAvailableSlots(pkg.getAvailableSlots() + booking.getPassengerCount());

            if ("SOLD_OUT".equals(pkg.getPackageStatus()) && pkg.getAvailableSlots() > 0) {
                pkg.setPackageStatus("AVAILABLE");
            }

            booking.setBookingStatus("CANCELED");
            bookingRepository.save(booking);
            count++;
        }
        return count;
    }

    // ----- DTOs INTERNOS -----

    public static class BookingPriceBreakdown {
        public final BigDecimal grossAmount;
        public final BigDecimal discountAmount;
        public final BigDecimal finalAmount;
        public final BigDecimal discountPercent;
        public final String discountReasons;
        public final List<AppliedDiscount> appliedDiscountsList;

        public BookingPriceBreakdown(BigDecimal grossAmount, BigDecimal discountAmount,
                                     BigDecimal finalAmount, BigDecimal discountPercent,
                                     String discountReasons, List<AppliedDiscount> appliedDiscountsList) {
            this.grossAmount = grossAmount;
            this.discountAmount = discountAmount;
            this.finalAmount = finalAmount;
            this.discountPercent = discountPercent;
            this.discountReasons = discountReasons;
            this.appliedDiscountsList = appliedDiscountsList;
        }
    }

    public static class AppliedDiscount {
        public DiscountConfigEntity discount;
        public BigDecimal amount;

        public AppliedDiscount(DiscountConfigEntity discount, BigDecimal amount) {
            this.discount = discount;
            this.amount = amount;
        }
    }
}