package br.com.moto.models.dto;

import br.com.moto.models.enums.OilType;

/** Intervalo de troca sugerido para um tipo de óleo: padrão e faixa em km, e meses. */
public record OilIntervalDTO(OilType type, int defaultKm, int minKm, int maxKm, int defaultMonths) {

    public static OilIntervalDTO de(final OilType tipo) {
        return new OilIntervalDTO(tipo, tipo.getDefaultKm(), tipo.getMinKm(), tipo.getMaxKm(), tipo.getDefaultMonths());
    }
}
