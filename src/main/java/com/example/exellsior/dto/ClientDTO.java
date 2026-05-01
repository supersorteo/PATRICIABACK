package com.example.exellsior.dto;

import com.example.exellsior.entity.Client;
import com.example.exellsior.entity.ClientVehicle;

import java.util.Date;
import java.util.List;

public record ClientDTO(
        Long id,
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
        List<ClientVehicle> clientVehicles,
        String paymentMethod,
        Integer clover,
        Date entryTimestamp,
        Long exitTimestamp,
        Long lastDayClosed
) {
    public static ClientDTO from(Client c) {
        return new ClientDTO(
                c.getId(),
                c.getCode(),
                c.getName(),
                c.getDni(),
                c.getPhoneIntl(),
                c.getPhoneRaw(),
                c.getPlate(),
                c.getNotes(),
                c.getSpaceKey(),
                c.getVehicle(),
                c.getCategory(),
                c.getPrice(),
                c.getClientVehicles(),
                c.getPaymentMethod(),
                c.getClover(),
                c.getEntryTimestamp(),
                c.getExitTimestamp(),
                c.getLastDayClosed()
        );
    }

    public static ClientDTO fromSummary(Client c) {
        return new ClientDTO(
                c.getId(),
                c.getCode(),
                c.getName(),
                c.getDni(),
                c.getPhoneIntl(),
                c.getPhoneRaw(),
                c.getPlate(),
                c.getNotes(),
                c.getSpaceKey(),
                c.getVehicle(),
                c.getCategory(),
                c.getPrice(),
                List.of(),
                c.getPaymentMethod(),
                c.getClover(),
                c.getEntryTimestamp(),
                c.getExitTimestamp(),
                c.getLastDayClosed()
        );
    }
}
