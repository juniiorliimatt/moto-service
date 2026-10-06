package br.com.moto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class MotoServiceApplication {
    public static void main(final String[] args) {
        SpringApplication.run(MotoServiceApplication.class, args);
    }
}
