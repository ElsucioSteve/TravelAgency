package com.travelagency.backend.services;

import com.travelagency.backend.entities.TravelPackageEntity;
import com.travelagency.backend.repositories.TravelPackageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class TravelPackageService {

    private final TravelPackageRepository travelPackageRepository;

    public TravelPackageService(TravelPackageRepository travelPackageRepository) {
        this.travelPackageRepository = travelPackageRepository;
    }

    // ----- LECTURAS -----

    public List<TravelPackageEntity> getAllVisiblePackages() {
        // Solo los paquetes que el negocio quiere mostrar al cliente
        return travelPackageRepository.findByPackageStatusAndIsVisibleWebTrue("AVAILABLE");
    }

    public List<TravelPackageEntity> getAllPackages() {
        // Para administradores: ven todo, incluso ocultos o cancelados
        return travelPackageRepository.findAll();
    }

    public TravelPackageEntity getPackageById(Long id) {
        return travelPackageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Travel package not found with id: " + id));
    }

    public List<TravelPackageEntity> searchPackages(String destination, BigDecimal minPrice, BigDecimal maxPrice, LocalDate startDate) {
        return travelPackageRepository.searchPackages(destination, minPrice, maxPrice, startDate);
    }

    // ----- ESCRITURAS -----

    public TravelPackageEntity createPackage(TravelPackageEntity travelPackage) {
        // REGLAS DE NEGOCIO
        validateBusinessRules(travelPackage);

        // Calcular duracion automaticamente si no viene
        if (travelPackage.getDurationDays() == null) {
            long days = ChronoUnit.DAYS.between(travelPackage.getStartDate(), travelPackage.getEndDate());
            travelPackage.setDurationDays((int) days);
        }

        // Al crear, los cupos disponibles son iguales al total
        if (travelPackage.getAvailableSlots() == null) {
            travelPackage.setAvailableSlots(travelPackage.getTotalSlots());
        }

        // Estado inicial por defecto
        if (travelPackage.getPackageStatus() == null) {
            travelPackage.setPackageStatus("AVAILABLE");
        }

        // Por defecto, visible en web
        if (travelPackage.getIsVisibleWeb() == null) {
            travelPackage.setIsVisibleWeb(true);
        }

        return travelPackageRepository.save(travelPackage);
    }

    public TravelPackageEntity updatePackage(Long id, TravelPackageEntity updated) {
        TravelPackageEntity existing = getPackageById(id);

        // No permitimos bajar los cupos totales por debajo de las reservas existentes
        int alreadyReserved = existing.getTotalSlots() - existing.getAvailableSlots();
        if (updated.getTotalSlots() != null && updated.getTotalSlots() < alreadyReserved) {
            throw new RuntimeException("Cannot reduce total slots below already reserved seats (" + alreadyReserved + ")");
        }

        // Actualizamos solo los campos permitidos
        existing.setName(updated.getName());
        existing.setDestination(updated.getDestination());
        existing.setDescription(updated.getDescription());
        existing.setStartDate(updated.getStartDate());
        existing.setEndDate(updated.getEndDate());
        existing.setBasePrice(updated.getBasePrice());
        existing.setIncludedServices(updated.getIncludedServices());
        existing.setConditions(updated.getConditions());
        existing.setRestrictions(updated.getRestrictions());
        existing.setTravelType(updated.getTravelType());
        existing.setSeason(updated.getSeason());
        existing.setCategory(updated.getCategory());
        existing.setPackageStatus(updated.getPackageStatus());
        existing.setIsVisibleWeb(updated.getIsVisibleWeb());

        // Si cambian los cupos totales, ajustamos los disponibles proporcionalmente
        if (updated.getTotalSlots() != null && !updated.getTotalSlots().equals(existing.getTotalSlots())) {
            int diff = updated.getTotalSlots() - existing.getTotalSlots();
            existing.setTotalSlots(updated.getTotalSlots());
            existing.setAvailableSlots(existing.getAvailableSlots() + diff);
        }

        validateBusinessRules(existing);
        return travelPackageRepository.save(existing);
    }

    public void deletePackage(Long id) {
        TravelPackageEntity existing = getPackageById(id);

        // Regla: si tiene reservas, no se borra, solo se marca como CANCELED
        int alreadyReserved = existing.getTotalSlots() - existing.getAvailableSlots();
        if (alreadyReserved > 0) {
            existing.setPackageStatus("CANCELED");
            existing.setIsVisibleWeb(false);
            travelPackageRepository.save(existing);
            return;
        }

        // Si no tiene reservas, eliminamos fisicamente
        travelPackageRepository.deleteById(id);
    }

    // ----- METODO PRIVADO DE VALIDACION -----

    private void validateBusinessRules(TravelPackageEntity pkg) {
        if (pkg.getBasePrice() == null || pkg.getBasePrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Base price must be greater than zero");
        }

        if (pkg.getTotalSlots() == null || pkg.getTotalSlots() <= 0) {
            throw new RuntimeException("Total slots must be greater than zero");
        }

        if (pkg.getEndDate().isBefore(pkg.getStartDate())) {
            throw new RuntimeException("End date must be after start date");
        }
    }

    // ----- METODO QUE USARA EL MODULO DE BOOKING -----

    public void decrementAvailableSlots(Long packageId, int amount) {
        TravelPackageEntity pkg = getPackageById(packageId);

        if (pkg.getAvailableSlots() < amount) {
            throw new RuntimeException("Not enough available slots. Requested: " + amount + ", available: " + pkg.getAvailableSlots());
        }

        pkg.setAvailableSlots(pkg.getAvailableSlots() - amount);

        // Si los cupos llegan a 0, marcar como SOLD_OUT automaticamente
        if (pkg.getAvailableSlots() == 0) {
            pkg.setPackageStatus("SOLD_OUT");
        }

        travelPackageRepository.save(pkg);
    }

    @Transactional
    public int expirePassedPackages() {
        LocalDate today = LocalDate.now();

        // Buscar paquetes AVAILABLE o SOLD_OUT cuya fecha de fin ya pasó
        List<TravelPackageEntity> expiredPackages = travelPackageRepository.findAll()
                .stream()
                .filter(p -> ("AVAILABLE".equals(p.getPackageStatus())
                        || "SOLD_OUT".equals(p.getPackageStatus()))
                        && p.getEndDate() != null
                        && p.getEndDate().isBefore(today))
                .toList();

        int count = 0;
        for (TravelPackageEntity pkg : expiredPackages) {
            pkg.setPackageStatus("EXPIRED");
            travelPackageRepository.save(pkg);
            count++;
        }
        return count;
    }
}