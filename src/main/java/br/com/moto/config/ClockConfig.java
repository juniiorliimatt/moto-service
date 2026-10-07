package br.com.moto.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Relógio injetável (testes usam {@code Clock.fixed}). O fuso é explícito porque o container roda
 * em UTC: sem ele, "hoje" viraria o dia seguinte a partir das 21h no fuso do usuário e erraria os
 * dias restantes da troca de óleo.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${app.timezone:America/Fortaleza}") final String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }
}
