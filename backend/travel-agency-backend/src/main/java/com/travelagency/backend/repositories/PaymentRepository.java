package com.travelagency.backend.repositories;

import com.travelagency.backend.entities.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {

    // Buscar el pago de una reserva especifica
    Optional<PaymentEntity> findByBookingId(Long bookingId);

    // Para reportes: pagos en un rango de fechas
    List<PaymentEntity> findByPaymentDateBetween(LocalDateTime start, LocalDateTime end);

    // Buscar por su transaction ID
    Optional<PaymentEntity> findByInternalTransactionId(String transactionId);
}