package com.travelagency.backend.repositories;

import com.travelagency.backend.entities.DiscountConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiscountConfigRepository extends JpaRepository<DiscountConfigEntity, Long> {

    // Buscar descuentos ACTIVOS de un tipo especifico
    List<DiscountConfigEntity> findByApplicationCriteriaAndDiscountStatus(String criteria, String status);

    // Buscar todos los activos
    List<DiscountConfigEntity> findByDiscountStatus(String status);
}