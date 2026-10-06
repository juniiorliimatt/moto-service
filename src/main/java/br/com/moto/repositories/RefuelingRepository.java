package br.com.moto.repositories;

import br.com.moto.models.entities.Refueling;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefuelingRepository extends JpaRepository<Refueling, UUID> {

    Page<Refueling> findByMotorcycleIdAndOwnerUsernameAndDateBetween(
            UUID motorcycleId, String ownerUsername, LocalDate from, LocalDate to, Pageable pageable);

    List<Refueling> findByMotorcycleIdAndOwnerUsernameOrderByDateAscOdometerKmAsc(UUID motorcycleId, String ownerUsername);

    Optional<Refueling> findByIdAndMotorcycleIdAndOwnerUsername(UUID id, UUID motorcycleId, String ownerUsername);
}
