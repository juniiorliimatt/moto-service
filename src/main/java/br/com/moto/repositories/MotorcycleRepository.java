package br.com.moto.repositories;

import br.com.moto.models.entities.Motorcycle;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MotorcycleRepository extends JpaRepository<Motorcycle, UUID> {

    Optional<Motorcycle> findByIdAndOwnerUsername(UUID id, String ownerUsername);

    List<Motorcycle> findByOwnerUsernameOrderByNicknameAsc(String ownerUsername);
}
