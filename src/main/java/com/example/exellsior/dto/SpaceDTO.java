package com.example.exellsior.dto;

import com.example.exellsior.entity.Space;

public record SpaceDTO(
        String key,
        String subsueloId,
        boolean occupied,
        boolean hold,
        Long clientId,
        Long startTime,
        String displayName,
        boolean whatsappSent
) {
    public static SpaceDTO from(Space s) {
        return new SpaceDTO(
                s.getKey(),
                s.getSubsueloId(),
                s.isOccupied(),
                s.isHold(),
                s.getClientId(),
                s.getStartTime(),
                s.getDisplayName(),
                s.isWhatsappSent()
        );
    }
}
