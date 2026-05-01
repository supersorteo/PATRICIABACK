package com.example.exellsior.services;

import com.example.exellsior.entity.Subsuelo;
import com.example.exellsior.repository.SpaceRepository;
import com.example.exellsior.repository.SubsueloRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SubsueloService {

    @Autowired
    private SubsueloRepository subsueloRepository;

    @Autowired
    private SpaceRepository spaceRepository;

    public List<Subsuelo> getAllSubsuelos() {
        return subsueloRepository.findAll();
    }

    public Subsuelo getById(String id) {
        return subsueloRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subsuelo no encontrado: " + id));
    }

    public Subsuelo saveSubsuelo(Subsuelo subsuelo) {
        return subsueloRepository.save(subsuelo);
    }

    public Subsuelo updateSubsuelo(String id, Subsuelo updatedSubsuelo) {
        Subsuelo existing = getById(id);
        existing.setLabel(updatedSubsuelo.getLabel());
        // Si agregas más campos editables en el futuro, ponlos aquí
        return subsueloRepository.save(existing);
    }

   

    @Transactional
    public void deleteSubsuelo(String id) {
        if (spaceRepository.existsBySubsueloIdAndOccupiedTrue(id)) {
            throw new RuntimeException("No se puede eliminar el subsuelo porque tiene espacios ocupados");
        }
        spaceRepository.deleteBySubsueloId(id);
        subsueloRepository.deleteById(id);
    }
}
