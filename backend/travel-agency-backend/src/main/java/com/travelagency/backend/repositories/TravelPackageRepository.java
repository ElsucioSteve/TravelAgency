package com.travelagency.backend.repositories;

import com.travelagency.backend.entities.TravelPackageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TravelPackageRepository extends JpaRepository<TravelPackageEntity, Long> {

    // METODO DERIVADO: Spring genera el SQL leyendo el nombre del metodo.
    // Traduce a: SELECT * FROM travel_package WHERE package_status = ? AND is_visible_web = ?
    List<TravelPackageEntity> findByPackageStatusAndIsVisibleWebTrue(String packageStatus);

    // METODO DERIVADO: filtrar por destino (busqueda parcial, sin importar mayusculas)
    // Traduce a: SELECT * FROM travel_package WHERE destination LIKE %?%
    List<TravelPackageEntity> findByDestinationContainingIgnoreCase(String destination);

    // QUERY MANUAL: cuando los filtros son complejos, usamos @Query con JPQL.
    // JPQL trabaja con ENTIDADES, no con tablas. Por eso se escribe TravelPackageEntity, no travel_package.
    @Query("SELECT p FROM TravelPackageEntity p WHERE " +
            "(:destination IS NULL OR LOWER(p.destination) LIKE LOWER(CONCAT('%', :destination, '%'))) AND " +
            "(:minPrice IS NULL OR p.basePrice >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.basePrice <= :maxPrice) AND " +
            "(:startDate IS NULL OR p.startDate >= :startDate) AND " +
            "p.packageStatus = 'AVAILABLE' AND " +
            "p.isVisibleWeb = true")
    List<TravelPackageEntity> searchPackages(
            @Param("destination") String destination,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("startDate") LocalDate startDate
    );
}