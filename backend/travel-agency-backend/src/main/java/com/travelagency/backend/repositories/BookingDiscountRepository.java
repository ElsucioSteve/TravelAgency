package com.travelagency.backend.repositories;

import com.travelagency.backend.entities.BookingDiscountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingDiscountRepository extends JpaRepository<BookingDiscountEntity, BookingDiscountEntity.BookingDiscountId> {
}