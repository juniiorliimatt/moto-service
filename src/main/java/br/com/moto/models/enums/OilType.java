package br.com.moto.models.enums;

import lombok.Getter;

/**
 * Tipo de óleo e o intervalo de troca <b>sugerido</b> (padrão + faixa em km, e meses). É só o
 * ponto de partida do formulário: cada troca grava o intervalo efetivamente escolhido.
 */
@Getter
public enum OilType {
    MINERAL(1500, 1000, 1500, 6),
    SEMI_SYNTHETIC(4000, 3000, 4000, 6),
    SYNTHETIC(6000, 5000, 6000, 12);

    private final int defaultKm;
    private final int minKm;
    private final int maxKm;
    private final int defaultMonths;

    OilType(final int defaultKm, final int minKm, final int maxKm, final int defaultMonths) {
        this.defaultKm = defaultKm;
        this.minKm = minKm;
        this.maxKm = maxKm;
        this.defaultMonths = defaultMonths;
    }
}
