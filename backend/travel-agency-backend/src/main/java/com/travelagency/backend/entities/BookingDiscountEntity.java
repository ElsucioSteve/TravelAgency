package com.travelagency.backend.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "booking_discount")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingDiscountEntity {

    @EmbeddedId
    private BookingDiscountId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("bookingId")
    @JoinColumn(name = "booking_id")
    @JsonBackReference
    @EqualsAndHashCode.Exclude
    private BookingEntity booking;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("discountId")
    @JoinColumn(name = "discount_id")
    private DiscountConfigEntity discount;

    @Column(nullable = false)
    private BigDecimal appliedAmount;

    // Clave primaria compuesta
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingDiscountId implements Serializable {
        private Long bookingId;
        private Long discountId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BookingDiscountId that)) return false;
            return Objects.equals(bookingId, that.bookingId) &&
                    Objects.equals(discountId, that.discountId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(bookingId, discountId);
        }
    }
}