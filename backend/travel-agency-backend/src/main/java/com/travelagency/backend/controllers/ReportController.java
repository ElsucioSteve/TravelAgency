package com.travelagency.backend.controllers;

import com.travelagency.backend.services.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // ----- REPORTE 1: VENTAS POR PERIODO -----

    @GetMapping("/sales")
    @PreAuthorize("hasRole('ADMIN')")
    public ReportService.SalesReport getSalesByPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        return reportService.getSalesByPeriod(startDate, endDate);
    }

    // ----- REPORTE 2: RANKING DE PAQUETES -----

    @GetMapping("/package-ranking")
    @PreAuthorize("hasRole('ADMIN')")
    public ReportService.PackageRankingReport getPackageRanking(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        return reportService.getPackageRanking(startDate, endDate);
    }
}