package br.com.moto.exceptions;

/** Hodômetro que quebra a regra de monotonicidade da moto (ver {@code OdometerRules}) — 400. */
public class InvalidOdometerException extends RuntimeException {
    public InvalidOdometerException(final String message) {
        super(message);
    }
}
