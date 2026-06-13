package com.travelagency.backend.services;

import com.travelagency.backend.entities.BookingEntity;
import com.travelagency.backend.repositories.BookingRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReportService {

    private final BookingRepository bookingRepository;

    public ReportService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    // ----- REPORTE 1: VENTAS POR PERIODO -----

    public SalesReport getSalesByPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        validateDateRange(startDate, endDate);

        List<BookingEntity> bookings = bookingRepository.findSalesByPeriod(startDate, endDate);

        // Construir items del reporte
        List<SalesReportItem> items = new ArrayList<>();
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalFinal = BigDecimal.ZERO;
        int totalPassengers = 0;

        for (BookingEntity b : bookings) {
            SalesReportItem item = new SalesReportItem();
            item.bookingId = b.getId();
            item.bookingCode = b.getBookingCode();
            item.bookingDate = b.getBookingDate();
            item.clientName = b.getUser().getFullName();
            item.clientEmail = b.getUser().getEmail();
            item.packageName = b.getTravelPackage().getName();
            item.destination = b.getTravelPackage().getDestination();
            item.passengerCount = b.getPassengerCount();
            item.grossAmount = b.getGrossAmount();
            item.discountAmount = b.getDiscountAmount();
            item.finalAmount = b.getFinalAmount();
            item.bookingStatus = b.getBookingStatus();
            items.add(item);

            // Acumular totales
            if (b.getGrossAmount() != null) totalGross = totalGross.add(b.getGrossAmount());
            if (b.getDiscountAmount() != null) totalDiscount = totalDiscount.add(b.getDiscountAmount());
            if (b.getFinalAmount() != null) totalFinal = totalFinal.add(b.getFinalAmount());
            totalPassengers += b.getPassengerCount();
        }

        // Armar reporte completo
        SalesReport report = new SalesReport();
        report.startDate = startDate;
        report.endDate = endDate;
        report.totalBookings = items.size();
        report.totalPassengers = totalPassengers;
        report.totalGrossAmount = totalGross;
        report.totalDiscountAmount = totalDiscount;
        report.totalFinalAmount = totalFinal;
        report.items = items;

        return report;
    }

    // ----- REPORTE 2: RANKING DE PAQUETES -----

    public PackageRankingReport getPackageRanking(LocalDateTime startDate, LocalDateTime endDate) {
        validateDateRange(startDate, endDate);

        List<Object[]> rawResults = bookingRepository.findPackageRankingByPeriod(startDate, endDate);

        // Convertir Object[] a DTOs limpios
        List<PackageRankingItem> items = new ArrayList<>();
        int rank = 1;
        for (Object[] row : rawResults) {
            PackageRankingItem item = new PackageRankingItem();
            item.rank = rank++;
            item.packageId = (Long) row[0];
            item.packageName = (String) row[1];
            item.destination = (String) row[2];
            item.totalBookings = (Long) row[3];
            items.add(item);
        }

        PackageRankingReport report = new PackageRankingReport();
        report.startDate = startDate;
        report.endDate = endDate;
        report.items = items;
        return report;
    }

    // ----- VALIDACION -----

    private void validateDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            throw new RuntimeException("Both startDate and endDate are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new RuntimeException("startDate must be before or equal to endDate");
        }
    }

    // ----- DTOs (clases internas para no crear archivos extras) -----

    public static class SalesReport {
        public LocalDateTime startDate;
        public LocalDateTime endDate;
        public int totalBookings;
        public int totalPassengers;
        public BigDecimal totalGrossAmount;
        public BigDecimal totalDiscountAmount;
        public BigDecimal totalFinalAmount;
        public List<SalesReportItem> items;
    }

    public static class SalesReportItem {
        public Long bookingId;
        public String bookingCode;
        public LocalDateTime bookingDate;
        public String clientName;
        public String clientEmail;
        public String packageName;
        public String destination;
        public Integer passengerCount;
        public BigDecimal grossAmount;
        public BigDecimal discountAmount;
        public BigDecimal finalAmount;
        public String bookingStatus;
    }

    public static class PackageRankingReport {
        public LocalDateTime startDate;
        public LocalDateTime endDate;
        public List<PackageRankingItem> items;
    }

    public static class PackageRankingItem {
        public int rank;
        public Long packageId;
        public String packageName;
        public String destination;
        public Long totalBookings;
    }
}