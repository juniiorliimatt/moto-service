package br.com.moto.config;

import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * {@code authentication.getName()} funciona para o {@code BearerTokenAuthentication} (token
 * opaco/introspecção). Nunca fazer cast para {@code Jwt}: o principal nunca é um. Vazio quando
 * não autenticado — todo endpoint exige autenticação, então esse ramo não ocorre na prática.
 */
@Component
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        final var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return Optional.ofNullable(authentication.getName());
    }
}
