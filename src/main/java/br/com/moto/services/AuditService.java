package br.com.moto.services;

import br.com.moto.config.audit.CustomRevisionEntity;
import br.com.moto.models.dto.MotorcycleRevisionDTO;
import br.com.moto.models.entities.Motorcycle;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Leitura do histórico de revisões gravado pelo Hibernate Envers ({@code @Audited}). Como a moto
 * pertence a um dono, o histórico só é liberado a ele: {@code exigirDoDono} faz a checagem
 * (404, não 403, se não for o dono) antes de consultar o Envers.
 */
@Service
public class AuditService {

    private final EntityManager entityManager;
    private final MotorcycleService motorcycleService;

    public AuditService(final EntityManager entityManager, final MotorcycleService motorcycleService) {
        this.entityManager = entityManager;
        this.motorcycleService = motorcycleService;
    }

    /** {@code exigirDoDono} não é descartável: é a checagem de dono/existência antes de consultar o Envers. */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<MotorcycleRevisionDTO> historicoMoto(final UUID id, final String owner) {
        motorcycleService.exigirDoDono(id, owner);

        final var reader = AuditReaderFactory.get(entityManager);
        final List<Object[]> rows = reader.createQuery()
                .forRevisionsOfEntity(Motorcycle.class, false, true)
                .add(AuditEntity.id().eq(id))
                .addOrder(AuditEntity.revisionNumber().asc())
                .getResultList();

        return rows.stream()
                .map(row -> {
                    final var snapshot = (Motorcycle) row[0];
                    final var revision = (CustomRevisionEntity) row[1];
                    final var type = (RevisionType) row[2];
                    return new MotorcycleRevisionDTO(
                            revision.getId(),
                            toLocalDateTime(revision.getTimestamp()),
                            revision.getUsername(),
                            type.name(),
                            snapshot.getId(),
                            snapshot.getNickname(),
                            snapshot.getModel(),
                            snapshot.getInitialOdometerKm(),
                            snapshot.isActive());
                })
                .toList();
    }

    private static LocalDateTime toLocalDateTime(final long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }
}
