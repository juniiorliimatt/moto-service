package br.com.moto.repositories;

import br.com.moto.models.entities.OilChange;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OilChangeRepository extends JpaRepository<OilChange, UUID> {

    List<OilChange> findByMotorcycleIdAndOwnerUsernameOrderByDateDescOdometerKmDesc(UUID motorcycleId, String ownerUsername);

    Optional<OilChange> findFirstByMotorcycleIdAndOwnerUsernameOrderByDateDescOdometerKmDesc(UUID motorcycleId, String ownerUsername);

    Optional<OilChange> findByIdAndMotorcycleIdAndOwnerUsername(UUID id, UUID motorcycleId, String ownerUsername);
}
