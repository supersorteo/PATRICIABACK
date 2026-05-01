package com.example.exellsior.controller;

import com.example.exellsior.dto.SpaceDTO;
import com.example.exellsior.entity.Space;
import com.example.exellsior.services.SpaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/spaces")
public class SpaceController {

    @Autowired
    private SpaceService spaceService;

    @GetMapping
    public List<SpaceDTO> getAll() {
        return spaceService.getAllSpaces().stream().map(SpaceDTO::from).toList();
    }

    @GetMapping("/{key}")
    public SpaceDTO getByKey(@PathVariable String key) {
        return SpaceDTO.from(spaceService.getByKey(key));
    }

    @PostMapping
    public SpaceDTO create(@RequestBody Space space) {
        return SpaceDTO.from(spaceService.saveSpace(space));
    }

    @PutMapping("/{key}")
    public SpaceDTO update(@PathVariable String key, @RequestBody Space updatedSpace) {
        return SpaceDTO.from(spaceService.updateSpace(key, updatedSpace));
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> delete(@PathVariable String key) {
        spaceService.deleteSpace(key);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{key}/transfer")
    public ResponseEntity<SpaceDTO> transferSpace(
            @PathVariable String key,
            @RequestBody Map<String, String> body
    ) {
        String newSubsueloId = body.get("newSubsueloId");
        if (newSubsueloId == null || newSubsueloId.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(SpaceDTO.from(spaceService.transferSpace(key, newSubsueloId)));
    }
}
