package com.travelagency.backend.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpirationSchedulerTest {

    @Mock
    private BookingService bookingService;

    @Mock
    private TravelPackageService travelPackageService;

    @InjectMocks
    private ExpirationScheduler scheduler;

    @Test
    @DisplayName("checkExpiredBookings() should delegate to bookingService")
        // Verifica que el scheduler de reservas llama al service correcto
    void checkExpiredBookings_ShouldCallBookingService() {
        when(bookingService.expirePendingBookings()).thenReturn(3);

        scheduler.checkExpiredBookings();

        verify(bookingService).expirePendingBookings();
    }

    @Test
    @DisplayName("checkExpiredPackages() should delegate to travelPackageService")
        // Verifica que el scheduler de paquetes llama al service correcto
    void checkExpiredPackages_ShouldCallPackageService() {
        when(travelPackageService.expirePassedPackages()).thenReturn(2);

        scheduler.checkExpiredPackages();

        verify(travelPackageService).expirePassedPackages();
    }

    @Test
    @DisplayName("checkExpiredBookings() should not throw on exception")
        // Si el service lanza excepcion, el scheduler la captura
    void checkExpiredBookings_WhenServiceThrows_ShouldNotPropagate() {
        when(bookingService.expirePendingBookings()).thenThrow(new RuntimeException("DB error"));

        // No debe lanzar excepcion al exterior
        scheduler.checkExpiredBookings();

        verify(bookingService).expirePendingBookings();
    }

    @Test
    @DisplayName("checkExpiredPackages() should not throw on exception")
        // Captura excepciones del service de paquetes
    void checkExpiredPackages_WhenServiceThrows_ShouldNotPropagate() {
        when(travelPackageService.expirePassedPackages()).thenThrow(new RuntimeException("DB error"));

        scheduler.checkExpiredPackages();

        verify(travelPackageService).expirePassedPackages();
    }
}