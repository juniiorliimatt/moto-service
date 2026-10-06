package br.com.moto.config.audit;

import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Instanciado pelo Hibernate, não pelo Spring — sem DI, por isso lê o SecurityContext
 * estaticamente em vez de injetar um AuditorAware.
 */
public class RevisionListenerImpl implements RevisionListener {

    @Override
    public void newRevision(final Object revisionEntity) {
        final var revision = (CustomRevisionEntity) revisionEntity;
        revision.setUsername(currentUsername());
    }

    private String currentUsername() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "system";
        }
        return authentication.getName();
    }
}
