package com.travelagency.backend.services;

import com.travelagency.backend.entities.BookingEntity;
import com.travelagency.backend.entities.TravelPackageEntity;
import com.travelagency.backend.entities.UserEntity;
import com.travelagency.backend.repositories.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private ReportService reportService;

    private BookingEntity booking;

    @BeforeEach
    void setUp() {
        UserEntity user = new UserEntity();
        user.setFullName("Test User");
        user.setEmail("test@test.com");

        TravelPackageEntity pkg = new TravelPackageEntity();
        pkg.setId(1L);
        pkg.setName("Tour Test");
        pkg.setDestination("Chile");

        booking = new BookingEntity();
        booking.setId(1L);
        booking.setBookingCode("BK-001");
        booking.setBookingDate(LocalDateTime.now());
        booking.setUser(user);
        booking.setTravelPackage(pkg);
        booking.setPassengerCount(2);
        booking.setGrossAmount(new BigDecimal("1000000"));
        booking.setDiscountAmount(new BigDecimal("100000"));
        booking.setFinalAmount(new BigDecimal("900000"));
        booking.setBookingStatus("CONFIRMED");
    }

    @Test
    @DisplayName("getSalesByPeriod() should return aggregated totals")
        // Suma totales de ventas en el periodo
    void getSalesByPeriod_ShouldAggregate() {
        when(bookingRepository.findSalesByPeriod(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(booking));

        ReportService.SalesReport report = reportService.getSalesByPeriod(
                LocalDateTime.now().minusDays(7),
                LocalDateTime.now()
        );

        assertThat(report.totalBookings).isEqualTo(1);
        assertThat(report.totalPassengers).isEqualTo(2);
        assertThat(report.totalGrossAmount).isEqualByComparingTo("1000000");
        assertThat(report.totalDiscountAmount).isEqualByComparingTo("100000");
        assertThat(report.totalFinalAmount).isEqualByComparingTo("900000");
        assertThat(report.items).hasSize(1);
    }

    @Test
    @DisplayName("getSalesByPeriod() with empty result should return zero totals")
        // Sin ventas en el periodo, todos los totales son 0
    void getSalesByPeriod_WhenEmpty_ShouldReturnZero() {
        when(bookingRepository.findSalesByPeriod(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        ReportService.SalesReport report = reportService.getSalesByPeriod(
                LocalDateTime.now().minusDays(7),
                LocalDateTime.now()
        );

        assertThat(report.totalBookings).isEqualTo(0);
        assertThat(report.totalPassengers).isEqualTo(0);
        assertThat(report.totalFinalAmount).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("getSalesByPeriod() should throw when startDate is null")
        // Las fechas son obligatorias
    void getSalesByPeriod_NullStartDate_ShouldThrow() {
        assertThatThrownBy(() -> reportService.getSalesByPeriod(null, LocalDateTime.now()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("required");
    }

    @Test
    @DisplayName("getSalesByPeriod() should throw when startDate is after endDate")
        // El rango debe ser coherente
    void getSalesByPeriod_StartAfterEnd_ShouldThrow() {
        assertThatThrownBy(() -> reportService.getSalesByPeriod(
                LocalDateTime.now(),
                LocalDateTime.now().minusDays(1)
        )).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("before");
    }

    @Test
    @DisplayName("getPackageRanking() should return ranked items")
        // Devuelve paquetes ordenados por cantidad de reservas
    void getPackageRanking_ShouldRank() {
        Object[] row1 = new Object[]{1L, "Tour A", "Chile", 5L};
        Object[] row2 = new Object[]{2L, "Tour B", "Mexico", 3L};

        when(bookingRepository.findPackageRankingByPeriod(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(row1, row2));

        ReportService.PackageRankingReport report = reportService.getPackageRanking(
                LocalDateTime.now().minusDays(30),
                LocalDateTime.now()
        );

        assertThat(report.items).hasSize(2);
        assertThat(report.items.get(0).rank).isEqualTo(1);
        assertThat(report.items.get(0).packageName).isEqualTo("Tour A");
        assertThat(report.items.get(0).totalBookings).isEqualTo(5L);
        assertThat(report.items.get(1).rank).isEqualTo(2);
    }

    @Test
    @DisplayName("getPackageRanking() with empty result")
        // Sin datos devuelve lista vacia
    void getPackageRanking_WhenEmpty_ShouldReturnEmpty() {
        when(bookingRepository.findPackageRankingByPeriod(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        ReportService.PackageRankingReport report = reportService.getPackageRanking(
                LocalDateTime.now().minusDays(30),
                LocalDateTime.now()
        );

        assertThat(report.items).isEmpty();
    }
}