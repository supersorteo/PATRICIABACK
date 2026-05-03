package com.example.exellsior.services;

import com.example.exellsior.entity.Space;
import com.example.exellsior.repository.SpaceRepository;
import com.example.exellsior.repository.SubsueloRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SpaceService {

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private SubsueloRepository subsueloRepository;

    public List<Space> getAllSpaces() {
        return spaceRepository.findAll();
    }

    public Space getByKey(String key) {
        return spaceRepository.findById(key)
                .orElseThrow(() -> new RuntimeException("Espacio no encontrado: " + key));
    }

    public Space saveSpace(Space space) {
        return spaceRepository.save(space);
    }


    public Space updateSpace(String key, Space updatedSpace) {
        Space existing = getByKey(key);

        // Campos permitidos para actualizar desde frontend
        existing.setOccupied(updatedSpace.isOccupied());
        existing.setHold(updatedSpace.isHold());
        existing.setClientId(updatedSpace.getClientId());
        existing.setStartTime(updatedSpace.getStartTime());
        existing.setDisplayName(updatedSpace.getDisplayName());
        existing.setWhatsappSent(updatedSpace.isWhatsappSent());

        // subsueloId no se cambia desde aquí (es fijo)
        return spaceRepository.save(existing);
    }



    public void deleteSpace(String key) {
        spaceRepository.deleteById(key);
    }


    @Transactional
    public Space transferSpace(String key, String newSubsueloId) {
        Space space = spaceRepository.findById(key)
                .orElseThrow(() -> new RuntimeException("Espacio no encontrado: " + key));

        if (space.isOccupied()) {
            throw new RuntimeException("No se puede transferir un espacio ocupado");
        }

        // Validar que subsuelo destino exista
        if (!subsueloRepository.existsById(newSubsueloId)) {
            throw new RuntimeException("Subsuelo destino no existe");
        }

        // Validar unicidad de key en destino
        if (spaceRepository.existsByKeyAndSubsueloId(key, newSubsueloId)) {
            throw new RuntimeException("La clave ya existe en el subsuelo destino");
        }

        // Validar unicidad de displayName en destino
        String displayName = space.getDisplayName() != null ? space.getDisplayName() : space.getKey();
        boolean nameExists = spaceRepository.existsByDisplayNameAndSubsueloId(displayName, newSubsueloId);
        if (nameExists) {
            throw new RuntimeException("Ya existe un espacio con ese nombre en el destino");
        }

        // Transferir
        space.setSubsueloId(newSubsueloId);
        return spaceRepository.save(space);
    }

}
