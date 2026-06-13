package com.travelagency.backend.controllers;

import com.travelagency.backend.entities.BookingEntity;
import com.travelagency.backend.entities.UserEntity;
import com.travelagency.backend.repositories.UserRepository;
import com.travelagency.backend.services.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;

    public BookingController(BookingService bookingService, UserRepository userRepository) {
        this.bookingService = bookingService;
        this.userRepository = userRepository;
    }

    // ----- ADMIN: ve todo -----

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<BookingEntity> getAllBookings() {
        return bookingService.getAllBookings();
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<BookingEntity> getBookingsByUserId(@PathVariable Long userId) {
        return bookingService.getBookingsByUserId(userId);
    }

    // ----- USUARIO AUTENTICADO: solo lo suyo -----

    @GetMapping("/{id}")
    public BookingEntity getBookingById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        BookingEntity booking = bookingService.getBookingById(id);
        ensureOwnerOrAdmin(booking, jwt);
        return booking;
    }

    @GetMapping("/my-bookings")
    public List<BookingEntity> getMyBookings(@AuthenticationPrincipal Jwt jwt) {
        UserEntity currentUser = getCurrentUser(jwt);
        return bookingService.getBookingsByUserId(currentUser.getId());
    }

    // ----- PREVIEW DE PRECIO -----
    // El userId YA NO se acepta del body. Se toma del JWT.

    @PostMapping("/calculate-preview")
    public ResponseEntity<Map<String, Object>> calculatePreview(
            @RequestBody BookingActionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UserEntity currentUser = getCurrentUser(jwt);

        BookingService.BookingPriceBreakdown breakdown = bookingService.calculatePrice(
                currentUser.getId(),
                request.packageId,
                request.passengerCount,
                request.promoDiscountId
        );

        return ResponseEntity.ok(Map.of(
                "grossAmount", breakdown.grossAmount,
                "discountAmount", breakdown.discountAmount,
                "finalAmount", breakdown.finalAmount,
                "discountPercent", breakdown.discountPercent,
                "discountReasons", breakdown.discountReasons
        ));
    }

    // ----- CREAR RESERVA -----
    // El userId YA NO se acepta del body. Se toma del JWT.

    @PostMapping
    public ResponseEntity<BookingEntity> createBooking(
            @RequestBody BookingActionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UserEntity currentUser = getCurrentUser(jwt);

        BookingEntity created = bookingService.createBooking(
                currentUser.getId(),
                request.packageId,
                request.passengerCount,
                request.passengers,
                request.promoDiscountId
        );
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    // ----- CANCELAR RESERVA -----

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelBooking(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        BookingEntity booking = bookingService.getBookingById(id);
        ensureOwnerOrAdmin(booking, jwt);

        bookingService.cancelBooking(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // ----- EXPIRAR PENDIENTES (admin) -----

    @PostMapping("/expire-pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> expirePendingNow() {
        int count = bookingService.expirePendingBookings();
        return ResponseEntity.ok(Map.of(
                "message", "Expiration check completed",
                "expiredBookings", count
        ));
    }

    // ----- METODOS DE SEGURIDAD -----

    private UserEntity getCurrentUser(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in DB: " + email));
    }

    private void ensureOwnerOrAdmin(BookingEntity booking, Jwt jwt) {
        // Verificar si es admin (los admins pueden ver todo)
        boolean isAdmin = jwt.getClaimAsMap("realm_access") != null &&
                ((List<?>) jwt.getClaimAsMap("realm_access").get("roles")).contains("ADMIN");

        if (isAdmin) return;

        // Si no es admin, validar que la reserva sea suya
        String email = jwt.getClaimAsString("email");
        if (!booking.getUser().getEmail().equals(email)) {
            throw new AccessDeniedException("You can only access your own bookings");
        }
    }

    // ----- DTO SIMPLIFICADO -----
    // Ya no incluye userId porque se obtiene del JWT.

    public static class BookingActionRequest {
        public Long packageId;
        public int passengerCount;
        public java.util.List<com.travelagency.backend.entities.BookingPassengerEntity> passengers;
        public Long promoDiscountId;
    }
}