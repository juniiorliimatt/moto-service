package br.com.moto.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Próxima troca de óleo por km <b>e</b> por tempo, o que vencer primeiro — puro, sem Spring nem banco. */
public final class OilStatusCalculator {

    /** Piso do alerta "perto" em km: vale o maior entre este e 10% do intervalo. */
    public static final int NEAR_KM_MIN = 500;
    /** Piso do alerta "perto" em dias: vale o maior entre este e 10% do intervalo. */
    public static final int NEAR_DAYS_MIN = 30;

    private OilStatusCalculator() {
    }

    /**
     * @param dataTroca       data da última troca
     * @param hodometroTroca  hodômetro da última troca
     * @param intervaloKm     intervalo escolhido em km
     * @param intervaloMeses  intervalo escolhido em meses
     * @param hodometroAtual  maior hodômetro conhecido da moto
     * @param hoje            data de referência
     */
    public static OilStatus calcular(final LocalDate dataTroca, final int hodometroTroca, final int intervaloKm,
                                     final int intervaloMeses, final int hodometroAtual, final LocalDate hoje) {
        final int vencimentoKm = hodometroTroca + intervaloKm;
        final LocalDate vencimentoData = dataTroca.plusMonths(intervaloMeses);
        final int kmRestantes = vencimentoKm - hodometroAtual;
        final long diasRestantes = ChronoUnit.DAYS.between(hoje, vencimentoData);
        final long diasDoIntervalo = ChronoUnit.DAYS.between(dataTroca, vencimentoData);

        final boolean vencida = kmRestantes <= 0 || diasRestantes <= 0;
        final boolean perto = kmRestantes <= Math.max(NEAR_KM_MIN, intervaloKm / 10)
                || diasRestantes <= Math.max(NEAR_DAYS_MIN, diasDoIntervalo / 10);
        final var nivel = vencida ? OilStatusLevel.VENCIDA : perto ? OilStatusLevel.PERTO : OilStatusLevel.OK;

        final double fracaoKm = (double) kmRestantes / intervaloKm;
        final double fracaoTempo = diasDoIntervalo == 0 ? 0 : (double) diasRestantes / diasDoIntervalo;
        final var limitante = fracaoKm <= fracaoTempo ? OilLimit.KM : OilLimit.TIME;

        return new OilStatus(vencimentoData, vencimentoKm, kmRestantes, diasRestantes, nivel, limitante);
    }
}
