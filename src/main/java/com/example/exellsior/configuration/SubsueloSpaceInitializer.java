package com.example.exellsior.configuration;

import com.example.exellsior.entity.Space;
import com.example.exellsior.entity.Subsuelo;
import com.example.exellsior.repository.SpaceRepository;
import com.example.exellsior.repository.SubsueloRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SubsueloSpaceInitializer {

    private final SubsueloRepository subsueloRepository;
    private final SpaceRepository spaceRepository;

    public SubsueloSpaceInitializer(SubsueloRepository subsueloRepository,
                                    SpaceRepository spaceRepository) {
        this.subsueloRepository = subsueloRepository;
        this.spaceRepository = spaceRepository;
    }

    @PostConstruct
    public void init() {
        if (subsueloRepository.count() > 0) {
            return;
        }

        Subsuelo sub = new Subsuelo();
        sub.setId("SUB1");
        sub.setLabel("Subsuelo 1");
        subsueloRepository.save(sub);

        for (int i = 1; i <= 5; i++) {
            String key = String.format("SUB1-%03d", i);
            Space space = new Space();
            space.setKey(key);
            space.setSubsueloId("SUB1");
            space.setOccupied(false);
            space.setHold(false);
            space.setWhatsappSent(false);
            spaceRepository.save(space);
        }

        log.info("Subsuelo inicial creado: SUB1 con 5 espacios (SUB1-001 a SUB1-005)");
    }
}
