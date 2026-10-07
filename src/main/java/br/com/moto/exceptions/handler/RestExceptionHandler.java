package br.com.moto.exceptions.handler;

import br.com.moto.exceptions.InvalidOdometerException;
import br.com.moto.exceptions.InvalidPeriodException;
import br.com.moto.exceptions.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Corpo de erro padronizado em RFC 9457 ({@link ProblemDetail}), com catch-all
 * {@link #handleUnexpected}: sem ele, exceção não mapeada cairia no whitelabel error do Spring,
 * potencialmente vazando stack trace.
 */
@RestControllerAdvice
public class RestExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(RestExceptionHandler.class);

    private final MessageSourceAccessor messages;

    public RestExceptionHandler(final MessageSourceAccessor messages) {
        this.messages = messages;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(final ResourceNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(InvalidOdometerException.class)
    public ProblemDetail handleInvalidOdometer(final InvalidOdometerException exception) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(InvalidPeriodException.class)
    public ProblemDetail handleInvalidPeriod(final InvalidPeriodException exception) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(final MethodArgumentNotValidException exception) {
        final var detail = problem(HttpStatus.BAD_REQUEST, messages.getMessage("erro.validacaoFalhou"));
        detail.setProperty("errors", exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(), "message", String.valueOf(error.getDefaultMessage())))
                .toList());
        return detail;
    }

    /** A mensagem original do Hibernate/Postgres expõe detalhe de implementação — troca por uma de alto nível; a original fica só no log. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(final DataIntegrityViolationException exception, final HttpServletRequest request) {
        logger.error("Data integrity violation on {}", request.getRequestURI(), exception);
        return problem(HttpStatus.CONFLICT, messages.getMessage("erro.registroEmUso"));
    }

    /** JSON malformado ou com shape errado é sempre erro do client — sem este handler o catch-all devolveria 500. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMessageNotReadable(final HttpMessageNotReadableException exception) {
        return problem(HttpStatus.BAD_REQUEST, messages.getMessage("erro.jsonMalFormado"));
    }

    /** Query param obrigatório ausente (ex.: {@code year}) é erro do client. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParameter(final MissingServletRequestParameterException exception) {
        return problem(HttpStatus.BAD_REQUEST, messages.getMessage("erro.parametroObrigatorio", new Object[]{exception.getParameterName()}));
    }

    /** Query/path param com tipo errado (ex.: {@code year=abc}, UUID inválido) também é 400. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(final MethodArgumentTypeMismatchException exception) {
        return problem(HttpStatus.BAD_REQUEST, messages.getMessage("erro.parametroInvalido", new Object[]{exception.getName()}));
    }

    /** Rota inexistente é erro do client; sem este handler o catch-all abaixo a transformaria em 500. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResource(final NoResourceFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, messages.getMessage("erro.rotaNaoEncontrada"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotSupported(final HttpRequestMethodNotSupportedException exception) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).headers(exception.getHeaders())
                .body(problem(HttpStatus.METHOD_NOT_ALLOWED, messages.getMessage("erro.metodoNaoSuportado")));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMediaTypeNotSupported(final HttpMediaTypeNotSupportedException exception) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).headers(exception.getHeaders())
                .body(problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, messages.getMessage("erro.tipoMidiaNaoSuportado")));
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(final Exception exception, final HttpServletRequest request) {
        logger.error("Unhandled exception on {}", request.getRequestURI(), exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, messages.getMessage("erro.inesperado"));
    }

    private ProblemDetail problem(final HttpStatus status, final String detail) {
        return ProblemDetail.forStatusAndDetail(status, detail);
    }
}
