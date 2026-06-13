package com.travelagency.backend.repositories;

import com.travelagency.backend.entities.BookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<BookingEntity, Long> {

    // Buscar todas las reservas de un usuario especifico (por su ID local)
    List<BookingEntity> findByUserId(Long userId);

    // Buscar reservas de un usuario filtradas por estado
    // (usado para cancelar PENDINGs cuando se desactiva un usuario)
    List<BookingEntity> findByUserIdAndBookingStatus(Long userId, String bookingStatus);

    // Contar cuantas reservas CONFIRMED tiene un usuario (para descuento de cliente frecuente)
    long countByUserIdAndBookingStatus(Long userId, String bookingStatus);

    // Buscar reservas por estado (para tareas batch tipo "cancelar las expiradas")
    List<BookingEntity> findByBookingStatus(String bookingStatus);

    // Buscar una reserva por su codigo unico
    Optional<BookingEntity> findByBookingCode(String bookingCode);

    // Reservas en un rango de fechas, excluyendo canceladas y expiradas
    @Query("SELECT b FROM BookingEntity b WHERE " +
            "b.bookingDate BETWEEN :startDate AND :endDate AND " +
            "b.bookingStatus = 'CONFIRMED'" +
            "ORDER BY b.bookingDate DESC")
    List<BookingEntity> findSalesByPeriod(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Ranking de paquetes: cuenta reservas activas por paquete en un periodo
    // Devuelve un array de Object[] con [packageId, packageName, destination, count]
    @Query("SELECT b.travelPackage.id, b.travelPackage.name, b.travelPackage.destination, COUNT(b) " +
            "FROM BookingEntity b WHERE " +
            "b.bookingDate BETWEEN :startDate AND :endDate AND " +
            "b.bookingStatus = 'CONFIRMED'" +
            "GROUP BY b.travelPackage.id, b.travelPackage.name, b.travelPackage.destination " +
            "ORDER BY COUNT(b) DESC, b.travelPackage.name ASC")
    List<Object[]> findPackageRankingByPeriod(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}