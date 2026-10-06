package br.com.moto.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Moto do usuário. {@code initialOdometerKm} é o piso do hodômetro — nenhum abastecimento, troca
 * de óleo ou leitura da moto pode ficar abaixo dele.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "motorcycles", schema = "moto")
@EntityListeners(AuditingEntityListener.class)
@Audited
public class Motorcycle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 60)
    private String nickname;

    @Column(length = 60)
    private String brand;

    @Column(nullable = false, length = 60)
    private String model;

    @Column(name = "model_year")
    private Integer modelYear;

    @Column(length = 10)
    private String plate;

    @Column(name = "initial_odometer_km", nullable = false)
    private int initialOdometerKm;

    @Column(name = "tank_capacity_liters", precision = 5, scale = 2)
    private BigDecimal tankCapacityLiters;

    @Column(nullable = false)
    private boolean active;

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
