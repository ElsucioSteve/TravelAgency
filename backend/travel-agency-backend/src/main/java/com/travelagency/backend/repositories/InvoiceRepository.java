package com.travelagency.backend.repositories;

import com.travelagency.backend.entities.InvoiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceEntity, Long> {

    // Buscar factura asociada a un pago
    Optional<InvoiceEntity> findByPaymentId(Long paymentId);

    // Buscar por folio (uso administrativo)
    Optional<InvoiceEntity> findByFolioNumber(String folioNumber);

    // Para generar el siguiente folio: cuántas facturas hay este año
    long countByInvoiceType(String invoiceType);
}