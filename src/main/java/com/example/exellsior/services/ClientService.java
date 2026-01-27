package com.example.exellsior.services;

import com.example.exellsior.entity.Client;
import com.example.exellsior.entity.Space;
import com.example.exellsior.entity.VehicleType;
import com.example.exellsior.repository.ClientRepository;
import com.example.exellsior.repository.SpaceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class ClientService {
    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private SpaceRepository spaceRepository;


    @Autowired
    private VehicleTypeService vehicleTypeService;

    public List<Client> getAllClients() {
        return clientRepository.findAll();
    }

    public Client getById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
    }

    @Transactional
    public Client saveClient(Client client) {
        // Si viene solo el ID del vehículo, cargar la entidad completa
        if (client.getVehicleType() != null && client.getVehicleType().getId() != null) {
            Long vehicleId = client.getVehicleType().getId();
            VehicleType vehicleType = vehicleTypeService.getById(vehicleId);
            client.setVehicleType(vehicleType);
        }
        return clientRepository.save(client);
    }

   /* @Transactional
    public Client updateClientPartially(Long id, Map<String, Object> updates) {
        Client client = getById(id);

        // Actualizar solo los campos que vienen
        if (updates.containsKey("price")) {
            client.setPrice((Integer) updates.get("price"));
        }
        if (updates.containsKey("code")) {
            client.setCode((String) updates.get("code"));
        }
        // Puedes agregar más si quieres (metodoPago, clover, etc.)

        // vehicleType: si viene ID, mantenerlo
        if (updates.containsKey("vehicleType")) {
            Object vt = updates.get("vehicleType");
            if (vt instanceof Map) {
                Long vtId = Long.valueOf(((Map<?, ?>) vt).get("id").toString());
                if (vtId != null) {
                    VehicleType vehicleType = vehicleTypeService.getById(vtId);
                    client.setVehicleType(vehicleType);
                }
            }
        }

        return clientRepository.save(client);
    }*/


    @Transactional
    public Client updateClientPartially(Long id, Map<String, Object> updates) {
        Client client = getById(id);

        if (updates.containsKey("price")) {
            client.setPrice((Integer) updates.get("price"));
        }
        if (updates.containsKey("code")) {
            client.setCode((String) updates.get("code"));
        }
        if (updates.containsKey("paymentMethod")) {
            client.setPaymentMethod((String) updates.get("paymentMethod"));
        }
        if (updates.containsKey("clover")) {
            Object cloverObj = updates.get("clover");
            client.setClover(cloverObj != null ? Integer.valueOf(cloverObj.toString()) : null);
        }
        if (updates.containsKey("vehicleType")) {
            Object vt = updates.get("vehicleType");
            if (vt instanceof Map) {
                Long vtId = Long.valueOf(((Map<?, ?>) vt).get("id").toString());
                if (vtId != null) {
                    VehicleType vehicleType = vehicleTypeService.getById(vtId);
                    client.setVehicleType(vehicleType);
                }
            }
        }

        return clientRepository.save(client);
    }



   /* public void deleteClient(Long id) {
        clientRepository.deleteById(id);
    }*/

    @Transactional
    public void deleteClient(Long id) {
        Client client = getById(id);

        // Buscar si tiene espacio asociado
        String spaceKey = client.getSpaceKey();
        if (spaceKey != null) {
            Space space = spaceRepository.findById(spaceKey).orElse(null);
            if (space != null) {
                space.setOccupied(false);
                space.setHold(false);
                space.setClientId(null);
                space.setStartTime(null);
                space.setClient(null);
                spaceRepository.save(space);
                System.out.println("Espacio " + spaceKey + " liberado al eliminar cliente " + id);
            }
        }

        // Eliminar cliente
        clientRepository.delete(client);
    }

    public Client getByDni(String dni) {
        return clientRepository.findByDni(dni).orElse(null);
    }



    @Transactional
    public Client reserveSpace00(String spaceKey, Client clientData) {
        Space targetSpace = spaceRepository.findById(spaceKey)
                .orElseThrow(() -> new RuntimeException("Espacio no encontrado: " + spaceKey));

        if (targetSpace.isOccupied()) {
            throw new RuntimeException("El espacio ya está ocupado");
        }

        Client client;

        // SI VIENE ID → ACTUALIZAR CLIENTE EXISTENTE
        if (clientData.getId() != null) {
            client = clientRepository.findById(clientData.getId())
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

            // LIBERAR ESPACIO ANTERIOR SI TENÍA UNO DIFERENTE
            String oldSpaceKey = client.getSpaceKey();
            if (oldSpaceKey != null && !oldSpaceKey.equals(spaceKey)) {
                Space oldSpace = spaceRepository.findById(oldSpaceKey).orElse(null);
                if (oldSpace != null) {
                    oldSpace.setOccupied(false);
                    oldSpace.setHold(false);
                    oldSpace.setClientId(null);
                    oldSpace.setStartTime(null);
                    spaceRepository.save(oldSpace);
                    System.out.println("Espacio anterior liberado: " + oldSpaceKey);
                }
            }
        } else {
            // NUEVO CLIENTE
            client = new Client();
        }

        // ACTUALIZAR DATOS DEL CLIENTE
        client.setName(clientData.getName());
        client.setDni(clientData.getDni());
        client.setPhoneRaw(clientData.getPhoneRaw());
        client.setPhoneIntl(clientData.getPhoneIntl());
        client.setCode(clientData.getCode());
        client.setVehicle(clientData.getVehicle());
        client.setPlate(clientData.getPlate());
        client.setNotes(clientData.getNotes());
        client.setCategory(clientData.getCategory());
        client.setPrice(clientData.getPrice());
        client.setSpaceKey(spaceKey);  // ← Actualizamos el spaceKey del cliente
        client.setClover(client.getClover());
        client.setEntryTimestamp(new Date());

        // VehicleType
        if (clientData.getVehicleType() != null && clientData.getVehicleType().getId() != null) {
            VehicleType vt = vehicleTypeService.getById(clientData.getVehicleType().getId());
            client.setVehicleType(vt);
        }

        // GUARDAR CLIENTE
        client = clientRepository.save(client);

        // OCUPAR ESPACIO NUEVO
        targetSpace.setOccupied(true);
        targetSpace.setHold(false);
        targetSpace.setClientId(client.getId());
        targetSpace.setStartTime(System.currentTimeMillis());
        spaceRepository.save(targetSpace);

        return client;
    }

    @Transactional
    public Client reserveSpace(String spaceKey, Client clientData) {
        Space targetSpace = spaceRepository.findById(spaceKey)
                .orElseThrow(() -> new RuntimeException("Espacio no encontrado: " + spaceKey));

        if (targetSpace.isOccupied()) {
            throw new RuntimeException("El espacio ya está ocupado");
        }

        Client client;

        // SI VIENE ID → ACTUALIZAR CLIENTE EXISTENTE
        if (clientData.getId() != null) {
            client = clientRepository.findById(clientData.getId())
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

            // LIBERAR ESPACIO ANTERIOR SI TENÍA UNO DIFERENTE
            String oldSpaceKey = client.getSpaceKey();
            if (oldSpaceKey != null && !oldSpaceKey.equals(spaceKey)) {
                Space oldSpace = spaceRepository.findById(oldSpaceKey).orElse(null);
                if (oldSpace != null) {
                    oldSpace.setOccupied(false);
                    oldSpace.setHold(false);
                    oldSpace.setClientId(null);
                    oldSpace.setStartTime(null);
                    spaceRepository.save(oldSpace);
                    System.out.println("Espacio anterior liberado: " + oldSpaceKey);
                }
            }
        } else {
            // NUEVO CLIENTE
            client = new Client();
        }

        // ACTUALIZAR DATOS DEL CLIENTE
        client.setName(clientData.getName());
        client.setDni(clientData.getDni());
        client.setPhoneRaw(clientData.getPhoneRaw());
        client.setPhoneIntl(clientData.getPhoneIntl());
        client.setCode(clientData.getCode());
        client.setVehicle(clientData.getVehicle());
        client.setPlate(clientData.getPlate());
        client.setNotes(clientData.getNotes());
        client.setCategory(clientData.getCategory());
        client.setPrice(clientData.getPrice());
        client.setSpaceKey(spaceKey);  // ← Actualizamos el spaceKey del cliente
        client.setClover(client.getClover());
        client.setEntryTimestamp(new Date());
        client.setExitTimestamp(null);

        // VehicleType
        if (clientData.getVehicleType() != null && clientData.getVehicleType().getId() != null) {
            VehicleType vt = vehicleTypeService.getById(clientData.getVehicleType().getId());
            client.setVehicleType(vt);
        }

        // GUARDAR CLIENTE
        client = clientRepository.save(client);

        // OCUPAR ESPACIO NUEVO
        targetSpace.setOccupied(true);
        targetSpace.setHold(false);
        targetSpace.setClientId(client.getId());
        targetSpace.setStartTime(System.currentTimeMillis());
        spaceRepository.save(targetSpace);

        return client;
    }



    @Transactional
    public void releaseSpace0(String spaceKey) {
        Space space = spaceRepository.findById(spaceKey)
                .orElseThrow(() -> new RuntimeException("Espacio no encontrado: " + spaceKey));

        Long clientId = space.getClientId();

        // Liberar space
        space.setOccupied(false);
        space.setHold(false);
        space.setClientId(null);
        space.setStartTime(null);
        spaceRepository.save(space);

        // Resetear datos de reserva en cliente (NO eliminar cliente)
        if (clientId != null) {
            Client client = clientRepository.findById(clientId)
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

            client.setSpaceKey(null);
            client.setCode(null);
            client.setVehicle(null);
            client.setCategory(null);
            client.setPrice(null);
            client.setVehicleType(null);
            client.setPlate(null);
            client.setNotes(null);
            client.setPaymentMethod(null);
            client.setClover(null);

            clientRepository.save(client);
        }
    }

    @Transactional
    public void releaseSpace1(String spaceKey) {
        Space space = spaceRepository.findById(spaceKey)
                .orElseThrow(() -> new RuntimeException("Espacio no encontrado: " + spaceKey));

        // Solo liberar el espacio
        space.setOccupied(false);
        space.setHold(false);
        space.setClientId(null);
        space.setStartTime(null);
        spaceRepository.save(space);

        // ← ELIMINAR TODO ESTO
        // NO tocar el cliente → queda con todos sus datos (precio, método de pago, clover, etc.)
        // if (clientId != null) { ... } ← BORRAR COMPLETO

        System.out.println("Espacio " + spaceKey + " liberado. Cliente mantiene todos sus datos para el reporte del día");
    }

    @Transactional
    public void releaseSpace(String spaceKey) {
        Space space = spaceRepository.findById(spaceKey)
                .orElseThrow(() -> new RuntimeException("Espacio no encontrado: " + spaceKey));

        Long clientId = space.getClientId();

        // Liberar espacio
        space.setOccupied(false);
        space.setHold(false);
        space.setClientId(null);
        space.setStartTime(null);
        spaceRepository.save(space);

        if (clientId != null) {
            Client client = clientRepository.findById(clientId)
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

           // client.setSpaceKey(null);
            client.setExitTimestamp(System.currentTimeMillis());  // ← REGISTRAR FECHA DE SALIDA

            // NO limpiar otros datos (price, vehicle, etc.)
            clientRepository.save(client);
        }

        System.out.println("Espacio liberado. Cliente " + clientId + " marcado con exitTimestamp");
    }

    @Transactional
    public void resetAllData1() {
        // 1. Liberar todos los espacios
        List<Space> allSpaces = spaceRepository.findAll();
        allSpaces.forEach(space -> {
            space.setOccupied(false);
            space.setHold(false);
            space.setClientId(null);
            space.setStartTime(null);
        });
        spaceRepository.saveAll(allSpaces);

        // 2. Limpiar datos de reserva en TODOS los clientes (sin borrarlos)
        List<Client> allClients = clientRepository.findAll();
        allClients.forEach(client -> {
            client.setSpaceKey(null);
            client.setCode(null);
            client.setVehicle(null);
            client.setCategory(null);
            client.setPrice(null);
            client.setVehicleType(null);
            client.setPlate(null);
            client.setNotes(null);
            client.setEntryTimestamp(null);
        });
        clientRepository.saveAll(allClients);

        System.out.println("Cierre del día completado: espacios liberados y datos de reserva limpiados en todos los clientes");
    }


    @Transactional
    public void resetAllData00() {
        // 1. Liberar todos los espacios
        List<Space> allSpaces = spaceRepository.findAll();
        allSpaces.forEach(space -> {
            space.setOccupied(false);
            space.setHold(false);
            space.setClientId(null);
            space.setStartTime(null);
        });
        spaceRepository.saveAll(allSpaces);

        // 2. SOLO resetear spaceKey en los clientes (nada más)
        List<Client> allClients = clientRepository.findAll();
        allClients.forEach(client -> {
            client.setSpaceKey(null);  // ← ÚNICA propiedad que se limpia
            client.setEntryTimestamp(null);
            client.setExitTimestamp(System.currentTimeMillis());
        });
        clientRepository.saveAll(allClients);

        System.out.println("Cierre del día completado: espacios liberados y spaceKey reseteado en clientes");
    }

    @Transactional
    public void resetAllData() {
        System.out.println("=== INICIO DEL CIERRE DEL DÍA ===");
        System.out.println("Fecha/Hora: " + new Date());

        // 1. Cargar todos los espacios y clientes
        System.out.println("Cargando espacios y clientes...");
        List<Space> allSpaces = spaceRepository.findAll();
        List<Client> allClients = clientRepository.findAll();

        List<Client> clientsToUpdate = new ArrayList<>();

        // 2. Liberar espacios (siempre)
        System.out.println("Liberando espacios...");
        for (Space space : allSpaces) {
            if (space.isOccupied()) {
                System.out.println(" → Liberando espacio ocupado: " + space.getKey());
                space.setOccupied(false);
                space.setHold(false);
                space.setClientId(null);
                space.setStartTime(null);
            } else {
                System.out.println(" → Espacio ya libre: " + space.getKey());
            }
        }
        spaceRepository.saveAll(allSpaces);

        // 3. Limpiar clientes que aún tienen spaceKey no null (inconsistencias)
        System.out.println("Buscando clientes con spaceKey no reseteado...");
        for (Client client : allClients) {
            if (client.getSpaceKey() != null) {
                System.out.println(" → Cliente con spaceKey pendiente: ID " + client.getId() +
                        " | spaceKey: " + client.getSpaceKey() +
                        " | entryTimestamp: " + client.getEntryTimestamp() +
                        " | exitTimestamp: " + client.getExitTimestamp());

                client.setSpaceKey(null);
                client.setEntryTimestamp(null);

                // Solo poner exitTimestamp si NO tenía salida previa
                if (client.getExitTimestamp() == null) {
                    long now = System.currentTimeMillis();
                    client.setExitTimestamp(now);
                    System.out.println("   → Salida forzada aplicada: " + new Date(now));
                } else {
                    System.out.println("   → Salida previa detectada (" + new Date(client.getExitTimestamp()) + "). No se actualiza.");
                }

                clientsToUpdate.add(client);
            }
        }

        // 4. Guardar clientes actualizados
        if (!clientsToUpdate.isEmpty()) {
            System.out.println("Guardando " + clientsToUpdate.size() + " clientes con campos reseteados...");
            clientRepository.saveAll(clientsToUpdate);
            System.out.println("Clientes guardados.");
        } else {
            System.out.println("No hay clientes con spaceKey pendiente.");
        }

        System.out.println("=== CIERRE DEL DÍA FINALIZADO ===");
        System.out.println("Espacios procesados: " + allSpaces.size());
        System.out.println("Clientes reseteados: " + clientsToUpdate.size());
        System.out.println("=================================");
    }


}
