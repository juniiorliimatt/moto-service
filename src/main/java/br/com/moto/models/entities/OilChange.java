package br.com.moto.models.entities;

import br.com.moto.models.enums.OilType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Troca de óleo com o intervalo (km e meses) efetivamente escolhido para a próxima. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "oil_changes", schema = "moto")
@EntityListeners(AuditingEntityListener.class)
@Audited
public class OilChange {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "motorcycle_id", nullable = false, updatable = false)
    private Motorcycle motorcycle;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "odometer_km", nullable = false)
    private int odometerKm;

    @Enumerated(EnumType.STRING)
    @Column(name = "oil_type", nullable = false, length = 30)
    private OilType oilType;

    @Column(length = 60)
    private String brand;

    @Column(length = 20)
    private String viscosity;

    @Column(precision = 10, scale = 2)
    private BigDecimal cost;

    @Column(name = "interval_km", nullable = false)
    private int intervalKm;

    @Column(name = "interval_months", nullable = false)
    private int intervalMonths;

    /** Username (subject da introspecção) do dono — nunca vem do client. */
    @Column(name = "owner_username", nullable = false, updatable = false, length = 255)
    private String ownerUsername;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 50)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 50)
    private String updatedBy;
}
