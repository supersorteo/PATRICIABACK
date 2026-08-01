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
        ensureDefaultSubsueloAndSpaces();
    }

    private void ensureDefaultSubsueloAndSpaces() {
        Subsuelo sub = subsueloRepository.findById("SUB1").orElseGet(() -> {
            Subsuelo newSub = new Subsuelo();
            newSub.setId("SUB1");
            newSub.setLabel("Subsuelo 1");
            return subsueloRepository.save(newSub);
        });

        for (int i = 1; i <= 5; i++) {
            String key = String.format("SUB1-%03d", i);
            if (spaceRepository.existsById(key)) {
                continue;
            }
            Space space = new Space();
            space.setKey(key);
            space.setSubsueloId(sub.getId());
            space.setDisplayName("SERVICIO " + i);
            space.setOccupied(false);
            space.setHold(false);
            space.setWhatsappSent(false);
            spaceRepository.save(space);
        }

        log.info("Subsuelo base verificado: SUB1 con espacios SERVICIO 1 a SERVICIO 5");
    }
}
