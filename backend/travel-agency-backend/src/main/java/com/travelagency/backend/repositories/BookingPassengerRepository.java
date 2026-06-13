package com.travelagency.backend.repositories;

import com.travelagency.backend.entities.BookingPassengerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingPassengerRepository extends JpaRepository<BookingPassengerEntity, Long> {
    List<BookingPassengerEntity> findByBookingId(Long bookingId);
}