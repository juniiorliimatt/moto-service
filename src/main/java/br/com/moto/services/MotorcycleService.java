package br.com.moto.services;

import br.com.moto.exceptions.ResourceNotFoundException;
import br.com.moto.models.dto.MotorcycleDTO;
import br.com.moto.models.dto.MotorcycleRequestDTO;
import br.com.moto.models.entities.Motorcycle;
import br.com.moto.repositories.MotorcycleRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MotorcycleService {

    private final MotorcycleRepository repository;
    private final MessageSourceAccessor messages;

    public MotorcycleService(final MotorcycleRepository repository, final MessageSourceAccessor messages) {
        this.repository = repository;
        this.messages = messages;
    }

    @Transactional(readOnly = true)
    public List<MotorcycleDTO> listar(final String owner) {
        return repository.findByOwnerUsernameOrderByNicknameAsc(owner).stream().map(MotorcycleService::paraDto).toList();
    }

    @Transactional(readOnly = true)
    public MotorcycleDTO buscar(final UUID id, final String owner) {
        return paraDto(exigirDoDono(id, owner));
    }

    @Transactional
    public MotorcycleDTO criar(final MotorcycleRequestDTO request, final String owner) {
        final var moto = Motorcycle.builder().ownerUsername(owner).build();
        aplicar(moto, request);
        return paraDto(repository.save(moto));
    }

    @Transactional
    public MotorcycleDTO atualizar(final UUID id, final MotorcycleRequestDTO request, final String owner) {
        final var moto = exigirDoDono(id, owner);
        aplicar(moto, request);
        return paraDto(repository.save(moto));
    }

    /** Apaga também abastecimentos, trocas de óleo e leituras da moto (FK {@code ON DELETE CASCADE}). */
    @Transactional
    public void excluir(final UUID id, final String owner) {
        repository.delete(exigirDoDono(id, owner));
    }

    /** Checagem de dono/existência (404, nunca 403) usada por todo recurso filho da moto. */
    @Transactional(readOnly = true)
    public Motorcycle exigirDoDono(final UUID id, final String owner) {
        return repository.findByIdAndOwnerUsername(id, owner)
                .orElseThrow(() -> new ResourceNotFoundException(messages.getMessage("moto.naoEncontrada")));
    }

    private static void aplicar(final Motorcycle moto, final MotorcycleRequestDTO request) {
        moto.setNickname(request.nickname().trim());
        moto.setBrand(request.brand());
        moto.setModel(request.model().trim());
        moto.setModelYear(request.modelYear());
        moto.setPlate(request.plate());
        moto.setInitialOdometerKm(request.initialOdometerKm());
        moto.setTankCapacityLiters(request.tankCapacityLiters());
        moto.setActive(request.active() == null || request.active());
    }

    static MotorcycleDTO paraDto(final Motorcycle moto) {
        return new MotorcycleDTO(moto.getId(), moto.getNickname(), moto.getBrand(), moto.getModel(), moto.getModelYear(),
                moto.getPlate(), moto.getInitialOdometerKm(), moto.getTankCapacityLiters(), moto.isActive());
    }
}
