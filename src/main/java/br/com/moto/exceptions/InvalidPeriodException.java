package br.com.moto.exceptions;

/** Período de consulta inválido (data inicial depois da final) — 400. */
public class InvalidPeriodException extends RuntimeException {
    public InvalidPeriodException(final String message) {
        super(message);
    }
}
