package br.com.moto.config;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;

/**
 * Aplicação é só pt-BR — {@link Locale} fixo, sem negociação por request. O
 * {@link LocalValidatorFactoryBean} aponta para o mesmo {@code messages.properties} para o Bean
 * Validation resolver {@code message = "{chave}"} sem um {@code ValidationMessages.properties}
 * separado, e o {@link FixedLocaleResolver} evita cair no texto em inglês do Hibernate
 * Validator quando o request não traz {@code Accept-Language}.
 */
@Configuration
public class MessageConfig {

    @Bean
    public MessageSourceAccessor messageSourceAccessor(final MessageSource messageSource) {
        return new MessageSourceAccessor(messageSource, Locale.of("pt", "BR"));
    }

    @Bean
    public LocalValidatorFactoryBean getValidator(final MessageSource messageSource) {
        final var validatorFactoryBean = new LocalValidatorFactoryBean();
        validatorFactoryBean.setValidationMessageSource(messageSource);
        return validatorFactoryBean;
    }

    @Bean
    public LocaleResolver localeResolver() {
        return new FixedLocaleResolver(Locale.of("pt", "BR"));
    }
}
