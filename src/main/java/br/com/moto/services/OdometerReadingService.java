package br.com.moto.services;

import br.com.moto.exceptions.ResourceNotFoundException;
import br.com.moto.models.dto.OdometerReadingDTO;
import br.com.moto.models.dto.OdometerReadingRequestDTO;
import br.com.moto.models.entities.OdometerReading;
import br.com.moto.repositories.OdometerReadingRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OdometerReadingService {

    private final OdometerReadingRepository repository;
    private final MotorcycleService motorcycleService;
    private final OdometerService odometerService;
    private final MessageSourceAccessor messages;

    public OdometerReadingService(final OdometerReadingRepository repository, final MotorcycleService motorcycleService,
                                  final OdometerService odometerService, final MessageSourceAccessor messages) {
        this.repository = repository;
        this.motorcycleService = motorcycleService;
        this.odometerService = odometerService;
        this.messages = messages;
    }

    @Transactional(readOnly = true)
    public List<OdometerReadingDTO> listar(final UUID motoId, final String owner) {
        motorcycleService.exigirDoDono(motoId, owner);
        return repository.findByMotorcycleIdAndOwnerUsernameOrderByDateAscOdometerKmAsc(motoId, owner).stream()
                .map(OdometerReadingService::paraDto).toList();
    }

    @Transactional
    public OdometerReadingDTO criar(final UUID motoId, final OdometerReadingRequestDTO request, final String owner) {
        final var moto = motorcycleService.exigirDoDono(motoId, owner);
        odometerService.validarNovo(motoId, owner, request.date(), request.odometerKm(), null);
        final var leitura = OdometerReading.builder()
                .motorcycle(moto)
                .date(request.date())
                .odometerKm(request.odometerKm())
                .ownerUsername(owner)
                .build();
        return paraDto(repository.save(leitura));
    }

    @Transactional
    public void excluir(final UUID motoId, final UUID id, final String owner) {
        motorcycleService.exigirDoDono(motoId, owner);
        final var leitura = repository.findByIdAndMotorcycleIdAndOwnerUsername(id, motoId, owner)
                .orElseThrow(() -> new ResourceNotFoundException(messages.getMessage("leitura.naoEncontrada")));
        repository.delete(leitura);
    }

    private static OdometerReadingDTO paraDto(final OdometerReading leitura) {
        return new OdometerReadingDTO(leitura.getId(), leitura.getDate(), leitura.getOdometerKm());
    }
}
