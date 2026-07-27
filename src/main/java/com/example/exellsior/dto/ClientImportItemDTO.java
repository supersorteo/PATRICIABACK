package com.example.exellsior.dto;

import java.util.ArrayList;
import java.util.List;

public class ClientImportItemDTO {
    private Long legacyId;
    private String name;
    private String dni;
    private String phoneIntl;
    private String phoneRaw;
    private List<ClientVehicleImportDTO> clientVehicles = new ArrayList<>();

    public Long getLegacyId() {
        return legacyId;
    }

    public void setLegacyId(Long legacyId) {
        this.legacyId = legacyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getPhoneIntl() {
        return phoneIntl;
    }

    public void setPhoneIntl(String phoneIntl) {
        this.phoneIntl = phoneIntl;
    }

    public String getPhoneRaw() {
        return phoneRaw;
    }

    public void setPhoneRaw(String phoneRaw) {
        this.phoneRaw = phoneRaw;
    }

    public List<ClientVehicleImportDTO> getClientVehicles() {
        return clientVehicles;
    }

    public void setClientVehicles(List<ClientVehicleImportDTO> clientVehicles) {
        this.clientVehicles = clientVehicles != null ? clientVehicles : new ArrayList<>();
    }

    public static class ClientVehicleImportDTO {
        private Long vehicleTypeId;
        private String plate;
        private String notes;

        public Long getVehicleTypeId() {
            return vehicleTypeId;
        }

        public void setVehicleTypeId(Long vehicleTypeId) {
            this.vehicleTypeId = vehicleTypeId;
        }

        public String getPlate() {
            return plate;
        }

        public void setPlate(String plate) {
            this.plate = plate;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }
}
