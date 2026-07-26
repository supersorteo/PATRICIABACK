package com.example.exellsior.configuration;

import com.example.exellsior.entity.AdminUser;
import com.example.exellsior.services.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DefaultAdminInitializer implements ApplicationRunner {

    private final AuthService authService;

    public DefaultAdminInitializer(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            AdminUser user = authService.ensureDefaultAdminUserIfMissing();
            log.info("[AUTH] Administrador por defecto verificado: {}", user.getUsername());
        } catch (RuntimeException ex) {
            log.info("[AUTH] Seed omitido: {}", ex.getMessage());
        }
    }
}
