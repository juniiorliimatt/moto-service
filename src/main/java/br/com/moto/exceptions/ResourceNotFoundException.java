package br.com.moto.exceptions;

/** Recurso inexistente <b>ou de outro dono</b> — sempre 404, nunca 403, para não revelar que existe. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(final String message) {
        super(message);
    }
}
