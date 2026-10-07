package br.com.moto.services;

import br.com.moto.domain.OdometerPoint;
import br.com.moto.domain.OdometerRules;
import br.com.moto.repositories.OdometerReadingRepository;
import br.com.moto.repositories.OilChangeRepository;
import br.com.moto.repositories.RefuelingRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Linha do tempo de hodômetro da moto: junta todas as fontes de km (leituras avulsas,
 * abastecimentos e trocas de óleo) e aplica {@link OdometerRules}.
 */
@Service
public class OdometerService {

    private final MotorcycleService motorcycleService;
    private final OdometerReadingRepository readingRepository;
    private final RefuelingRepository refuelingRepository;
    private final OilChangeRepository oilChangeRepository;

    public OdometerService(final MotorcycleService motorcycleService, final OdometerReadingRepository readingRepository,
                           final RefuelingRepository refuelingRepository, final OilChangeRepository oilChangeRepository) {
        this.motorcycleService = motorcycleService;
        this.readingRepository = readingRepository;
        this.refuelingRepository = refuelingRepository;
        this.oilChangeRepository = oilChangeRepository;
    }

    /** Todos os registros de hodômetro da moto, de qualquer fonte. 404 se a moto não for do dono. */
    @Transactional(readOnly = true)
    public List<OdometerPoint> pontos(final UUID motoId, final String owner) {
        motorcycleService.exigirDoDono(motoId, owner);
        return carregarPontos(motoId, owner);
    }

    /** Valida um km novo (ou editado) contra os demais registros; {@code ignorarId} exclui da comparação o registro que está sendo editado. */
    @Transactional(readOnly = true)
    public void validarNovo(final UUID motoId, final String owner, final LocalDate data, final int km, final UUID ignorarId) {
        final var moto = motorcycleService.exigirDoDono(motoId, owner);
        final var outros = new ArrayList<>(carregarPontos(motoId, owner));
        outros.removeIf(ponto -> Objects.equals(ponto.id(), ignorarId));
        OdometerRules.validar(outros, moto.getInitialOdometerKm(), data, km);
    }

    @Transactional(readOnly = true)
    public int hodometroAtual(final UUID motoId, final String owner) {
        final var moto = motorcycleService.exigirDoDono(motoId, owner);
        return OdometerRules.atual(carregarPontos(motoId, owner), moto.getInitialOdometerKm());
    }

    private List<OdometerPoint> carregarPontos(final UUID motoId, final String owner) {
        final var pontos = new ArrayList<OdometerPoint>();
        readingRepository.findByMotorcycleIdAndOwnerUsernameOrderByDateAscOdometerKmAsc(motoId, owner)
                .forEach(leitura -> pontos.add(new OdometerPoint(leitura.getId(), leitura.getDate(), leitura.getOdometerKm())));
        refuelingRepository.findByMotorcycleIdAndOwnerUsernameOrderByDateAscOdometerKmAsc(motoId, owner)
                .forEach(abastecimento -> pontos.add(
                        new OdometerPoint(abastecimento.getId(), abastecimento.getDate(), abastecimento.getOdometerKm())));
        oilChangeRepository.findByMotorcycleIdAndOwnerUsernameOrderByDateDescOdometerKmDesc(motoId, owner)
                .forEach(troca -> pontos.add(new OdometerPoint(troca.getId(), troca.getDate(), troca.getOdometerKm())));
        return pontos;
    }
}
