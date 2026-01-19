package com.example.exellsior.configuration;

import com.example.exellsior.entity.VehicleType;
import com.example.exellsior.services.VehicleTypeService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VehicleTypeInitializer {
    private final VehicleTypeService vehicleTypeService;

    public VehicleTypeInitializer(VehicleTypeService vehicleTypeService) {
        this.vehicleTypeService = vehicleTypeService;
    }

    @PostConstruct
    public void initVehicleTypes() {
        List<VehicleType> defaultVehicles = List.of(
                // SUV - $40.000
                new VehicleType("Toyota Corolla Cross", "SUV", 40000),
                new VehicleType("Chevrolet Tracker", "SUV", 40000),
                new VehicleType("Chevrolet Spin", "SUV", 40000),
                new VehicleType("Peugeot 2008", "SUV", 40000),
                new VehicleType("Peugeot 3008", "SUV", 40000),
                new VehicleType("Peugeot 5008", "SUV", 40000),
                new VehicleType("Volkswagen Taos", "SUV", 40000),
                new VehicleType("Volkswagen Nivus", "SUV", 40000),
                new VehicleType("Volkswagen Tiguan", "SUV", 40000),
                new VehicleType("Renault Kardian", "SUV", 40000),
                new VehicleType("Jeep Compass", "SUV", 40000),
                new VehicleType("Jeep Renegade", "SUV", 40000),
                new VehicleType("Fiat Fastback", "SUV", 40000),
                new VehicleType("Volkswagen T-Cross", "SUV", 40000),
                new VehicleType("Nissan Kicks", "SUV", 40000),
                new VehicleType("Ford Territory", "SUV", 40000),
                new VehicleType("Fiat Pulse", "SUV", 40000),
                new VehicleType("Citroën C3 Aircross", "SUV", 40000),
                new VehicleType("Ford Bronco Sport", "SUV", 40000),
                new VehicleType("Honda CRV", "SUV", 40000),
                new VehicleType("BMW X1", "SUV", 40000),
                new VehicleType("BMW X3", "SUV", 40000),
                new VehicleType("Hyundai Tucson", "SUV", 40000),
                new VehicleType("Kia Sportage", "SUV", 40000),
                new VehicleType("Audi Q3", "SUV", 40000),
                new VehicleType("Audi Q5", "SUV", 40000),
                new VehicleType("Mercedes Benz GLA", "SUV", 40000),
                new VehicleType("Mercedes Benz GLC", "SUV", 40000),
                new VehicleType("Chery Tiggo", "SUV", 40000),
                new VehicleType("Honda HRV", "SUV", 40000),
                new VehicleType("Toyota Rav4", "SUV", 40000),

                // PICKUP - $70.000
                new VehicleType("Mercedes Benz GLE", "PICKUP", 70000),
                new VehicleType("BMW X5", "PICKUP", 70000),
                new VehicleType("Chevrolet S10", "PICKUP", 70000),
                new VehicleType("Ford Ranger", "PICKUP", 70000),
                new VehicleType("Nissan Frontier", "PICKUP", 70000),
                new VehicleType("Toyota Hilux", "PICKUP", 70000),
                new VehicleType("Toyota SW4", "PICKUP", 70000),
                //new VehicleType("Toyota Rav4", "PICKUP", 70000),
                new VehicleType("Volkswagen Amarok", "PICKUP", 70000),
                new VehicleType("Jeep Grand Cherokee", "PICKUP", 70000),
                new VehicleType("Audi Q7", "PICKUP", 70000),
                new VehicleType("Renault Alaskan", "PICKUP", 70000),
                new VehicleType("Fiat Toro", "PICKUP", 70000),
                new VehicleType("Ford Everest", "PICKUP", 70000),
                new VehicleType("Ford Maverick", "PICKUP", 70000),
                new VehicleType("Hyundai Santa Fe", "PICKUP", 70000),
                new VehicleType("Kia Sorento", "PICKUP", 70000),
                new VehicleType("Nissan Xtrail", "PICKUP", 70000),
                new VehicleType("Chevrolet Blazer", "PICKUP", 70000),

                // ALTO PORTE - $100.000
                new VehicleType("Ram", "ALTO PORTE", 100000),
                new VehicleType("Ford F-150", "ALTO PORTE", 100000),
                new VehicleType("Ford F-150 Raptor", "ALTO PORTE", 100000),
                new VehicleType("Ford F-150 Tremor", "ALTO PORTE", 100000),
                new VehicleType("Chevrolet Montana", "ALTO PORTE", 100000),
                new VehicleType("Chevrolet Silverado", "ALTO PORTE", 100000),

                // MOTO - $35.000
                new VehicleType("Todas las motos", "MOTO", 35000),

                // AUTO - $35.000
                new VehicleType("Chevrolet Onix", "AUTO", 35000),
                new VehicleType("Chevrolet Cruze", "AUTO", 35000),
                new VehicleType("Toyota Corolla", "AUTO", 35000),
                new VehicleType("Toyota Etios", "AUTO", 35000),
                new VehicleType("Toyota Yaris", "AUTO", 35000),
                new VehicleType("Fiat Cronos", "AUTO", 35000),
                new VehicleType("Fiat Argo", "AUTO", 35000),
                new VehicleType("Peugeot 208", "AUTO", 35000),
                new VehicleType("Nissan Sentra", "AUTO", 35000),
                new VehicleType("Volkswagen Gol", "AUTO", 35000),
                new VehicleType("Volkswagen Golf", "AUTO", 35000),
                new VehicleType("Volkswagen Vento", "AUTO", 35000),
                new VehicleType("Volkswagen Passat", "AUTO", 35000),
                new VehicleType("Volkswagen Polo", "AUTO", 35000),
                new VehicleType("Volkswagen Virtus", "AUTO", 35000),
                new VehicleType("Ford Ka", "AUTO", 35000),
                //new VehicleType("Honda HRV", "AUTO", 35000),
                new VehicleType("Honda Civic", "AUTO", 35000),
                new VehicleType("Audi A3", "AUTO", 35000),
                new VehicleType("Audi A4", "AUTO", 35000),
                new VehicleType("Audi A5", "AUTO", 35000),
                new VehicleType("BMW Serie 1", "AUTO", 35000),
                new VehicleType("BMW Serie 2", "AUTO", 35000),
                new VehicleType("BMW Serie 3", "AUTO", 35000),
                new VehicleType("BMW Serie 4", "AUTO", 35000),
                new VehicleType("BMW Serie 5", "AUTO", 35000),
                new VehicleType("BMW X2", "AUTO", 35000),
                new VehicleType("Renault Clio", "AUTO", 35000),
                new VehicleType("Renault Kwid", "AUTO", 35000),
                new VehicleType("Renault Logan", "AUTO", 35000),
                new VehicleType("Renault Sandero", "AUTO", 35000),
                new VehicleType("Renault Stepway", "AUTO", 35000),
                new VehicleType("Mercedes Benz Clase A", "AUTO", 35000),
                new VehicleType("Mercedes Benz Clase C", "AUTO", 35000),
                new VehicleType("Mercedes Benz Clase E", "AUTO", 35000),
                new VehicleType("Audi Q2", "AUTO", 35000),
                new VehicleType("Nissan Versa", "AUTO", 35000),
                new VehicleType("Nissan March", "AUTO", 35000),
                new VehicleType("Citroën Berlingo", "AUTO", 35000),
                new VehicleType("Peugeot Partner", "AUTO", 35000),
                new VehicleType("Citroën C3", "AUTO", 35000),
                new VehicleType("Citroën C4", "AUTO", 35000),
                new VehicleType("Citroën Cactus", "AUTO", 35000),
                new VehicleType("Citroën Basalt", "AUTO", 35000),
                new VehicleType("Hyundai Kona", "AUTO", 35000),
                new VehicleType("Peugeot 308", "AUTO", 35000),
                new VehicleType("VW Fox", "AUTO", 35000),
                new VehicleType("Ford Mondeo", "AUTO", 35000),
                new VehicleType("Chevrolet Aguile", "AUTO", 35000),
                new VehicleType("Ford Fiesta 308", "AUTO", 35000),
                new VehicleType("Peugeot 207", "AUTO", 35000),
                new VehicleType("Suzuki Jimmy", "AUTO", 35000)
        );

        // Solo cargar si la tabla está vacía
        if (vehicleTypeService.getAll().isEmpty()) {
            defaultVehicles.forEach(v -> {
                try {
                    vehicleTypeService.create(v);
                } catch (Exception e) {
                    System.out.println("Error al insertar vehículo: " + v.getModel() + " - " + e.getMessage());
                }
            });
            System.out.println("Todos los vehículos por defecto cargados correctamente");
        }
    }

}
