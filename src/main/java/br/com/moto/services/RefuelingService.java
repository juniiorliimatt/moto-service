package br.com.moto.services;

import br.com.moto.domain.FuelEntry;
import br.com.moto.exceptions.ResourceNotFoundException;
import br.com.moto.models.dto.RefuelingDTO;
import br.com.moto.models.dto.RefuelingRequestDTO;
import br.com.moto.models.entities.Refueling;
import br.com.moto.repositories.RefuelingRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefuelingService {

    private static final LocalDate SEM_LIMITE_INFERIOR = LocalDate.of(1900, 1, 1);
    private static final LocalDate SEM_LIMITE_SUPERIOR = LocalDate.of(9999, 12, 31);

    private final RefuelingRepository repository;
    private final MotorcycleService motorcycleService;
    private final OdometerService odometerService;
    private final MessageSourceAccessor messages;

    public RefuelingService(final RefuelingRepository repository, final MotorcycleService motorcycleService,
                            final OdometerService odometerService, final MessageSourceAccessor messages) {
        this.repository = repository;
        this.motorcycleService = motorcycleService;
        this.odometerService = odometerService;
        this.messages = messages;
    }

    /** Página de abastecimentos da moto; {@code de}/{@code ate} nulos = sem limite. */
    @Transactional(readOnly = true)
    public Page<RefuelingDTO> listar(final UUID motoId, final String owner, final LocalDate de, final LocalDate ate,
                                     final Pageable pageable) {
        motorcycleService.exigirDoDono(motoId, owner);
        return repository.findByMotorcycleIdAndOwnerUsernameAndDateBetween(motoId, owner,
                de != null ? de : SEM_LIMITE_INFERIOR, ate != null ? ate : SEM_LIMITE_SUPERIOR, pageable)
                .map(RefuelingService::paraDto);
    }

    @Transactional
    public RefuelingDTO criar(final UUID motoId, final RefuelingRequestDTO request, final String owner) {
        final var moto = motorcycleService.exigirDoDono(motoId, owner);
        odometerService.validarNovo(motoId, owner, request.date(), request.odometerKm(), null);
        final var abastecimento = Refueling.builder().motorcycle(moto).ownerUsername(owner).build();
        aplicar(abastecimento, request);
        return paraDto(repository.save(abastecimento));
    }

    @Transactional
    public RefuelingDTO atualizar(final UUID motoId, final UUID id, final RefuelingRequestDTO request, final String owner) {
        motorcycleService.exigirDoDono(motoId, owner);
        final var abastecimento = exigir(motoId, id, owner);
        odometerService.validarNovo(motoId, owner, request.date(), request.odometerKm(), id);
        aplicar(abastecimento, request);
        return paraDto(repository.save(abastecimento));
    }

    @Transactional
    public void excluir(final UUID motoId, final UUID id, final String owner) {
        motorcycleService.exigirDoDono(motoId, owner);
        repository.delete(exigir(motoId, id, owner));
    }

    /** Abastecimentos da moto por data e hodômetro, ascendente — a entrada do cálculo de consumo. */
    @Transactional(readOnly = true)
    public List<FuelEntry> entradasOrdenadas(final UUID motoId, final String owner) {
        motorcycleService.exigirDoDono(motoId, owner);
        return repository.findByMotorcycleIdAndOwnerUsernameOrderByDateAscOdometerKmAsc(motoId, owner).stream()
                .map(a -> new FuelEntry(a.getDate(), a.getOdometerKm(), a.getLiters(), a.getTotalValue(), a.isFullTank()))
                .toList();
    }

    private Refueling exigir(final UUID motoId, final UUID id, final String owner) {
        return repository.findByIdAndMotorcycleIdAndOwnerUsername(id, motoId, owner)
                .orElseThrow(() -> new ResourceNotFoundException(messages.getMessage("abastecimento.naoEncontrado")));
    }

    private static void aplicar(final Refueling abastecimento, final RefuelingRequestDTO request) {
        abastecimento.setDate(request.date());
        abastecimento.setOdometerKm(request.odometerKm());
        abastecimento.setLiters(request.liters());
        abastecimento.setTotalValue(request.totalValue());
        abastecimento.setStation(request.station());
        abastecimento.setFuelType(request.fuelType());
        abastecimento.setFullTank(Boolean.TRUE.equals(request.fullTank()));
    }

    private static RefuelingDTO paraDto(final Refueling a) {
        return new RefuelingDTO(a.getId(), a.getDate(), a.getOdometerKm(), a.getLiters(), a.getTotalValue(),
                a.getTotalValue().divide(a.getLiters(), 3, RoundingMode.HALF_UP), a.getStation(), a.getFuelType(), a.isFullTank());
    }
}
