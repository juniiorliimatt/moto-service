package br.com.moto.services;

import br.com.moto.domain.OilStatusCalculator;
import br.com.moto.exceptions.ResourceNotFoundException;
import br.com.moto.models.dto.OilChangeDTO;
import br.com.moto.models.dto.OilChangeRequestDTO;
import br.com.moto.models.dto.OilStatusDTO;
import br.com.moto.models.entities.OilChange;
import br.com.moto.repositories.OilChangeRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OilChangeService {

    private final OilChangeRepository repository;
    private final MotorcycleService motorcycleService;
    private final OdometerService odometerService;
    private final MessageSourceAccessor messages;

    public OilChangeService(final OilChangeRepository repository, final MotorcycleService motorcycleService,
                            final OdometerService odometerService, final MessageSourceAccessor messages) {
        this.repository = repository;
        this.motorcycleService = motorcycleService;
        this.odometerService = odometerService;
        this.messages = messages;
    }

    /** Todas as trocas da moto, mais recente primeiro (poucas por moto, por natureza — sem paginação). */
    @Transactional(readOnly = true)
    public List<OilChangeDTO> listar(final UUID motoId, final String owner) {
        motorcycleService.exigirDoDono(motoId, owner);
        return repository.findByMotorcycleIdAndOwnerUsernameOrderByDateDescOdometerKmDesc(motoId, owner).stream()
                .map(OilChangeService::paraDto).toList();
    }

    @Transactional
    public OilChangeDTO criar(final UUID motoId, final OilChangeRequestDTO request, final String owner) {
        final var moto = motorcycleService.exigirDoDono(motoId, owner);
        odometerService.validarNovo(motoId, owner, request.date(), request.odometerKm(), null);
        final var troca = OilChange.builder().motorcycle(moto).ownerUsername(owner).build();
        aplicar(troca, request);
        return paraDto(repository.save(troca));
    }

    @Transactional
    public OilChangeDTO atualizar(final UUID motoId, final UUID id, final OilChangeRequestDTO request, final String owner) {
        motorcycleService.exigirDoDono(motoId, owner);
        final var troca = exigir(motoId, id, owner);
        odometerService.validarNovo(motoId, owner, request.date(), request.odometerKm(), id);
        aplicar(troca, request);
        return paraDto(repository.save(troca));
    }

    @Transactional
    public void excluir(final UUID motoId, final UUID id, final String owner) {
        motorcycleService.exigirDoDono(motoId, owner);
        repository.delete(exigir(motoId, id, owner));
    }

    /** Próxima troca: a da última troca registrada contra o hodômetro atual (de qualquer fonte) e a data de {@code hoje}. */
    @Transactional(readOnly = true)
    public OilStatusDTO status(final UUID motoId, final String owner, final LocalDate hoje) {
        motorcycleService.exigirDoDono(motoId, owner);
        final int hodometroAtual = odometerService.hodometroAtual(motoId, owner);
        return repository.findFirstByMotorcycleIdAndOwnerUsernameOrderByDateDescOdometerKmDesc(motoId, owner)
                .map(ultima -> {
                    final var status = OilStatusCalculator.calcular(ultima.getDate(), ultima.getOdometerKm(),
                            ultima.getIntervalKm(), ultima.getIntervalMonths(), hodometroAtual, hoje);
                    return new OilStatusDTO(paraDto(ultima), hodometroAtual, status.dueDate(), status.dueKm(),
                            status.kmRemaining(), status.daysRemaining(), status.level(), status.limitedBy());
                })
                .orElseGet(() -> new OilStatusDTO(null, hodometroAtual, null, null, null, null, null, null));
    }

    private OilChange exigir(final UUID motoId, final UUID id, final String owner) {
        return repository.findByIdAndMotorcycleIdAndOwnerUsername(id, motoId, owner)
                .orElseThrow(() -> new ResourceNotFoundException(messages.getMessage("oleo.naoEncontrada")));
    }

    private static void aplicar(final OilChange troca, final OilChangeRequestDTO request) {
        troca.setDate(request.date());
        troca.setOdometerKm(request.odometerKm());
        troca.setOilType(request.oilType());
        troca.setBrand(request.brand());
        troca.setViscosity(request.viscosity());
        troca.setCost(request.cost());
        troca.setIntervalKm(request.intervalKm());
        troca.setIntervalMonths(request.intervalMonths());
    }

    private static OilChangeDTO paraDto(final OilChange t) {
        return new OilChangeDTO(t.getId(), t.getDate(), t.getOdometerKm(), t.getOilType(), t.getBrand(), t.getViscosity(),
                t.getCost(), t.getIntervalKm(), t.getIntervalMonths());
    }
}
