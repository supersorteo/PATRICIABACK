package com.example.exellsior.dto;

import com.example.exellsior.entity.ServiceHistory;

public record ServiceHistoryDTO(
        Long id,
        Long sourceClientId,
        String code,
        String name,
        String dni,
        String phoneIntl,
        String phoneRaw,
        String plate,
        String notes,
        String spaceKey,
        String vehicle,
        String category,
        Integer price,
        String paymentMethod,
        Integer clover,
        Long entryTimestamp,
        Long exitTimestamp,
        String serviceDate,
        String archivedBy
) {
    public static ServiceHistoryDTO from(ServiceHistory entity) {
        return new ServiceHistoryDTO(
                entity.getId(),
                entity.getSourceClientId(),
                entity.getCode(),
                entity.getName(),
                entity.getDni(),
                entity.getPhoneIntl(),
                entity.getPhoneRaw(),
                entity.getPlate(),
                entity.getNotes(),
                entity.getSpaceKey(),
                entity.getVehicle(),
                entity.getCategory(),
                entity.getPrice(),
                entity.getPaymentMethod(),
                entity.getClover(),
                entity.getEntryTimestamp(),
                entity.getExitTimestamp(),
                entity.getServiceDate() != null ? entity.getServiceDate().toString() : null,
                entity.getArchivedBy()
        );
    }
}
