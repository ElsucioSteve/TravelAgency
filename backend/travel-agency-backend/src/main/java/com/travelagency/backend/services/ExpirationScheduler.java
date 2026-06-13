package com.travelagency.backend.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExpirationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ExpirationScheduler.class);

    private final BookingService bookingService;
    private final TravelPackageService travelPackageService;

    public ExpirationScheduler(BookingService bookingService,
                               TravelPackageService travelPackageService) {
        this.bookingService = bookingService;
        this.travelPackageService = travelPackageService;
    }

    // Reservas: cada 5 minutos
    @Scheduled(cron = "0 */5 * * * *")
    public void checkExpiredBookings() {
        try {
            int expired = bookingService.expirePendingBookings();
            if (expired > 0) {
                logger.info("Scheduler: {} reservas marcadas como EXPIRED", expired);
            }
        } catch (Exception e) {
            logger.error("Error en checkExpiredBookings", e);
        }
    }

    // Paquetes: cada hora (basta con menor frecuencia, son fechas en días)
    @Scheduled(cron = "0 0 * * * *")
    public void checkExpiredPackages() {
        try {
            int expired = travelPackageService.expirePassedPackages();
            if (expired > 0) {
                logger.info("Scheduler: {} paquetes marcados como EXPIRED", expired);
            }
        } catch (Exception e) {
            logger.error("Error en checkExpiredPackages", e);
        }
    }
}