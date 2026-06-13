package com.travelagency.backend.controllers;

import com.travelagency.backend.entities.InvoiceEntity;
import com.travelagency.backend.services.InvoiceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    // ----- ADMIN: lista global -----

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<InvoiceEntity> getAllInvoices() {
        return invoiceService.getAllInvoices();
    }

    // ----- USUARIO AUTENTICADO: con validacion de ownership -----

    @GetMapping("/{id}")
    public InvoiceEntity getInvoiceById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        InvoiceEntity invoice = invoiceService.getInvoiceById(id);
        ensureOwnerOrAdmin(invoice, jwt);
        return invoice;
    }

    @GetMapping("/folio/{folio}")
    public ResponseEntity<InvoiceEntity> getByFolio(@PathVariable String folio, @AuthenticationPrincipal Jwt jwt) {
        Optional<InvoiceEntity> invoice = invoiceService.getInvoiceByFolio(folio);
        if (invoice.isEmpty()) return ResponseEntity.notFound().build();

        ensureOwnerOrAdmin(invoice.get(), jwt);
        return ResponseEntity.ok(invoice.get());
    }

    @GetMapping("/payment/{paymentId}")
    public ResponseEntity<InvoiceEntity> getByPaymentId(@PathVariable Long paymentId, @AuthenticationPrincipal Jwt jwt) {
        Optional<InvoiceEntity> invoice = invoiceService.getInvoiceByPaymentId(paymentId);
        if (invoice.isEmpty()) return ResponseEntity.notFound().build();

        ensureOwnerOrAdmin(invoice.get(), jwt);
        return ResponseEntity.ok(invoice.get());
    }

    // ----- SEGURIDAD -----

    private void ensureOwnerOrAdmin(InvoiceEntity invoice, Jwt jwt) {
        boolean isAdmin = jwt.getClaimAsMap("realm_access") != null &&
                ((List<?>) jwt.getClaimAsMap("realm_access").get("roles")).contains("ADMIN");

        if (isAdmin) return;

        String email = jwt.getClaimAsString("email");
        String ownerEmail = invoice.getPayment().getBooking().getUser().getEmail();
        if (!ownerEmail.equals(email)) {
            throw new AccessDeniedException("You can only access your own invoices");
        }
    }
}