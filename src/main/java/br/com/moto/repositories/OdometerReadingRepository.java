package br.com.moto.repositories;

import br.com.moto.models.entities.OdometerReading;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OdometerReadingRepository extends JpaRepository<OdometerReading, UUID> {

    List<OdometerReading> findByMotorcycleIdAndOwnerUsernameOrderByDateAscOdometerKmAsc(UUID motorcycleId, String ownerUsername);

    Optional<OdometerReading> findByIdAndMotorcycleIdAndOwnerUsername(UUID id, UUID motorcycleId, String ownerUsername);
}
