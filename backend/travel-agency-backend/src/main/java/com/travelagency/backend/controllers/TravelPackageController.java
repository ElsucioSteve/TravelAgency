package com.travelagency.backend.controllers;

import com.travelagency.backend.entities.TravelPackageEntity;
import com.travelagency.backend.services.TravelPackageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/travel-packages")
public class TravelPackageController {

    private final TravelPackageService travelPackageService;

    public TravelPackageController(TravelPackageService travelPackageService) {
        this.travelPackageService = travelPackageService;
    }

    // ----- LECTURAS (cualquier autenticado) -----

    @GetMapping
    public List<TravelPackageEntity> getAllPackages() {
        return travelPackageService.getAllVisiblePackages();
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public List<TravelPackageEntity> getAllIncludingHidden() {
        return travelPackageService.getAllPackages();
    }

    @GetMapping("/{id}")
    public TravelPackageEntity getPackageById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        TravelPackageEntity pkg = travelPackageService.getPackageById(id);

        // Si el usuario NO es admin, validar que el paquete sea visible
        boolean isAdmin = jwt.getClaimAsMap("realm_access") != null &&
                ((java.util.List<?>) jwt.getClaimAsMap("realm_access").get("roles")).contains("ADMIN");

        if (!isAdmin) {
            if (!Boolean.TRUE.equals(pkg.getIsVisibleWeb()) ||
                    "EXPIRED".equals(pkg.getPackageStatus()) ||
                    "CANCELED".equals(pkg.getPackageStatus())) {
                throw new RuntimeException("Package not found");
            }
        }

        return pkg;
    }

    @GetMapping("/search")
    public List<TravelPackageEntity> search(
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate
    ) {
        return travelPackageService.searchPackages(destination, minPrice, maxPrice, startDate);
    }

    // ----- ESCRITURAS (solo ADMIN) -----

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TravelPackageEntity> createPackage(@RequestBody TravelPackageEntity travelPackage) {
        TravelPackageEntity created = travelPackageService.createPackage(travelPackage);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public TravelPackageEntity updatePackage(@PathVariable Long id, @RequestBody TravelPackageEntity travelPackage) {
        return travelPackageService.updatePackage(id, travelPackage);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePackage(@PathVariable Long id) {
        travelPackageService.deletePackage(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/expire-passed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> expirePassedNow() {
        int count = travelPackageService.expirePassedPackages();
        return ResponseEntity.ok(Map.of(
                "message", "Package expiration check completed",
                "expiredPackages", count
        ));
    }
}