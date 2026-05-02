package com.example.exellsior.dto;

import com.example.exellsior.entity.Client;
import com.example.exellsior.entity.ClientVehicle;

import java.util.Date;
import java.util.List;
import java.util.Map;

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

    public static ClientDTO fromMap(Map<String, Object> m) {
        if (m == null) return null;
        Long entryEpoch = toLong(m.get("entryTimestamp"));
        return new ClientDTO(
                toLong(m.get("id")),
                str(m.get("code")),
                str(m.get("name")),
                str(m.get("dni")),
                str(m.get("phoneIntl")),
                str(m.get("phoneRaw")),
                str(m.get("plate")),
                str(m.get("notes")),
                str(m.get("spaceKey")),
                str(m.get("vehicle")),
                str(m.get("category")),
                toInt(m.get("price")),
                List.of(),
                str(m.get("paymentMethod")),
                toInt(m.get("clover")),
                entryEpoch != null ? new Date(entryEpoch) : null,
                toLong(m.get("exitTimestamp")),
                null
        );
    }

    private static String str(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return (s.isEmpty() || "null".equalsIgnoreCase(s)) ? null : s;
    }

    private static Long toLong(Object v) {
        if (v == null) return null;
        try { return Long.parseLong(String.valueOf(v).trim()); } catch (Exception e) { return null; }
    }

    private static Integer toInt(Object v) {
        if (v == null) return null;
        try { return Integer.parseInt(String.valueOf(v).trim()); } catch (Exception e) { return null; }
    }
}
