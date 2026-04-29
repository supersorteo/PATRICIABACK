package com.example.exellsior.services;

import com.example.exellsior.dto.PagedResponse;
import com.example.exellsior.entity.Client;
import com.example.exellsior.entity.ClientVehicle;
import com.example.exellsior.entity.Space;
import com.example.exellsior.entity.VehicleType;
import com.example.exellsior.repository.ClientRepository;
import com.example.exellsior.repository.SpaceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;

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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
    }

    private List<VehicleType> resolveVehicleTypes(List<VehicleType> input) {
        if (input == null) return new ArrayList<>();
        List<VehicleType> resolved = new ArrayList<>();
        for (VehicleType vt : input) {
            if (vt != null && vt.getId() != null) {
                resolved.add(vehicleTypeService.getById(vt.getId()));
            }
        }
        return resolved;
    }

    private List<ClientVehicle> resolveClientVehicles(Client client, List<ClientVehicle> input) {
        if (input == null) return new ArrayList<>();

        List<ClientVehicle> resolved = new ArrayList<>();

        for (ClientVehicle cv : input) {
            if (cv == null || cv.getVehicleType() == null || cv.getVehicleType().getId() == null) continue;

            VehicleType vt = vehicleTypeService.getById(cv.getVehicleType().getId());

            ClientVehicle entity = new ClientVehicle();
            entity.setClient(client);
            entity.setVehicleType(vt);
            entity.setPlate(cv.getPlate());
            entity.setNotes(cv.getNotes());

            resolved.add(entity);
        }

        if (resolved.size() > 4) {
            throw new RuntimeException("Solo se permiten hasta 4 vehículos por cliente");
        }

        return resolved;
    }


    private void replaceClientVehicles(Client client, List<ClientVehicle> resolved) {
        List<ClientVehicle> current = client.getClientVehicles();
        if (current == null) {
            current = new ArrayList<>();
            client.setClientVehicles(current);
        } else {
            current.clear();
        }
        current.addAll(resolved);
    }




    private void ensureMax4(List<VehicleType> list) {
        if (list.size() > 4) {
            throw new RuntimeException("Solo se permiten hasta 4 vehículos por cliente");
        }
    }



    @Transactional
    public Client saveClient(Client client) {
        List<ClientVehicle> resolved = resolveClientVehicles(client, client.getClientVehicles());
        replaceClientVehicles(client, resolved);
        return clientRepository.save(client);
    }



    @Transactional
    public Client updateClientPartially(Long id, Map<String, Object> updates) {
        Client client = getById(id);

        if (updates.containsKey("price")) client.setPrice((Integer) updates.get("price"));
        if (updates.containsKey("code")) client.setCode((String) updates.get("code"));
        if (updates.containsKey("paymentMethod")) client.setPaymentMethod((String) updates.get("paymentMethod"));

        if (updates.containsKey("clover")) {
            Object cloverObj = updates.get("clover");
            client.setClover(cloverObj != null ? Integer.valueOf(cloverObj.toString()) : null);
        }

        if (updates.containsKey("clientVehicles")) {
            Object vts = updates.get("clientVehicles");
            if (vts instanceof List) {
                List<?> list = (List<?>) vts;
                List<ClientVehicle> incoming = new ArrayList<>();

                for (Object item : list) {
                    if (item instanceof Map) {
                        Map<?, ?> m = (Map<?, ?>) item;
                        Object vtObj = m.get("vehicleType");

                        if (vtObj instanceof Map) {
                            Object idObj = ((Map<?, ?>) vtObj).get("id");
                            if (idObj != null) {
                                ClientVehicle cv = new ClientVehicle();
                                VehicleType vt = vehicleTypeService.getById(Long.valueOf(idObj.toString()));
                                cv.setVehicleType(vt);
                                cv.setPlate(m.get("plate") != null ? m.get("plate").toString() : null);
                                cv.setNotes(m.get("notes") != null ? m.get("notes").toString() : null);
                                incoming.add(cv);
                            }
                        }
                    }
                }

                List<ClientVehicle> resolved = resolveClientVehicles(client, incoming);
                replaceClientVehicles(client, resolved);
            }
        }

        return clientRepository.save(client);
    }


    @Transactional
    public void deleteClient(Long id) {
        Client client = getById(id);
        List<Client> clientsToDelete = findClientsByIdentity(client);

        if (clientsToDelete.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado");
        }

        releaseSpacesForClients(clientsToDelete);
        clientRepository.deleteAll(clientsToDelete);
    }




    // ClientService.java
    public Client getByDni(String dni) {
        return clientRepository.findFirstByDniOrderByIdDesc(dni).orElse(null);
    }


    public List<Client> getReservationsByDni(String dni) {
        if (dni == null || dni.trim().isEmpty()) return List.of();
        return clientRepository.findByDniOrderByIdDesc(dni.trim());
    }




    @Transactional
    public Client reserveSpace(String spaceKey, Client clientData) {
        Space targetSpace = spaceRepository.findById(spaceKey)
                .orElseThrow(() -> new RuntimeException("Espacio no encontrado: " + spaceKey));

        if (targetSpace.isOccupied()) {
            throw new RuntimeException("El espacio ya está ocupado");
        }

        Client client;
        if (clientData.getId() != null) {
            client = clientRepository.findById(clientData.getId())
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

            String oldSpaceKey = client.getSpaceKey();
            if (oldSpaceKey != null && !oldSpaceKey.equals(spaceKey)) {
                Space oldSpace = spaceRepository.findById(oldSpaceKey).orElse(null);
                if (oldSpace != null) {
                    oldSpace.setOccupied(false);
                    oldSpace.setHold(false);
                    oldSpace.setClientId(null);
                    oldSpace.setStartTime(null);
                    spaceRepository.save(oldSpace);
                }
            }
        } else {
            client = new Client();
        }

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
        client.setSpaceKey(spaceKey);
        client.setClover(clientData.getClover());
        client.setEntryTimestamp(new Date());
        client.setExitTimestamp(null);

        // ✅ reemplazo seguro
        List<ClientVehicle> resolved = resolveClientVehicles(client, clientData.getClientVehicles());
        replaceClientVehicles(client, resolved);

        client = clientRepository.save(client);

        targetSpace.setOccupied(true);
        targetSpace.setHold(false);
        targetSpace.setClientId(client.getId());
        targetSpace.setStartTime(System.currentTimeMillis());
        spaceRepository.save(targetSpace);

        return client;
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
    public void resetAllData() {
        System.out.println("=== INICIO DEL CIERRE DEL DÍA ===");
        long nowMs = System.currentTimeMillis();
        System.out.println("Fecha/Hora: " + new Date(nowMs));

        // 1) Cargar solo espacios con estado operativo pendiente
        List<Space> spacesToReset = spaceRepository.findByOccupiedTrueOrHoldTrueOrClientIdIsNotNullOrStartTimeIsNotNull();

        // 2) Capturar clientIds vinculados a espacios antes de limpiar (para no depender solo de spaceKey)
        Set<Long> affectedClientIds = new HashSet<>();
        int spacesReset = spacesToReset.size();

        System.out.println("Liberando espacios...");
        for (Space space : spacesToReset) {
            if (space.getClientId() != null) {
                affectedClientIds.add(space.getClientId());
            }

            {
                System.out.println(" → Reseteando espacio: " + space.getKey() +
                        " | occupied=" + space.isOccupied() +
                        " | hold=" + space.isHold() +
                        " | clientId=" + space.getClientId() +
                        " | startTime=" + space.getStartTime());
            }

            // Limpieza completa del espacio
            space.setOccupied(false);
            space.setHold(false);
            space.setClientId(null);
            space.setStartTime(null);
            space.setClient(null); // importante para evitar referencia colgada
        }
        if (!spacesToReset.isEmpty()) {
            spaceRepository.saveAll(spacesToReset);
        }

        // 3) Limpiar clientes asociados por spaceKey o por clientId detectado en espacios
        List<Client> clientsToUpdate = new ArrayList<>();
        System.out.println("Buscando clientes para resetear...");

        List<Client> candidateClients = affectedClientIds.isEmpty()
                ? clientRepository.findBySpaceKeyIsNotNull()
                : clientRepository.findBySpaceKeyIsNotNullOrIdIn(affectedClientIds);

        for (Client client : candidateClients) {
            boolean hasSpaceKey = client.getSpaceKey() != null && !client.getSpaceKey().isBlank();
            boolean wasLinkedBySpace = client.getId() != null && affectedClientIds.contains(client.getId());

            if (hasSpaceKey || wasLinkedBySpace) {
                System.out.println(" → Reseteando cliente ID " + client.getId() +
                        " | spaceKey=" + client.getSpaceKey() +
                        " | entryTimestamp=" + client.getEntryTimestamp() +
                        " | exitTimestamp=" + client.getExitTimestamp());

                client.setSpaceKey(null);
                client.setEntryTimestamp(null);

                // Si no tenía salida, se fuerza al momento del cierre
                if (client.getExitTimestamp() == null) {
                    client.setExitTimestamp(nowMs);
                    System.out.println("   → exitTimestamp forzado: " + new Date(nowMs));
                }

                // Marca de último cierre diario aplicado
                client.setLastDayClosed(nowMs);

                clientsToUpdate.add(client);
            }
        }

        if (!clientsToUpdate.isEmpty()) {
            System.out.println("Guardando " + clientsToUpdate.size() + " clientes reseteados...");
            clientRepository.saveAll(clientsToUpdate);
        } else {
            System.out.println("No hubo clientes para resetear.");
        }

        System.out.println("=== CIERRE DEL DÍA FINALIZADO ===");
        System.out.println("Espacios procesados: " + spacesToReset.size());
        System.out.println("Espacios reseteados: " + spacesReset);
        System.out.println("Clientes reseteados: " + clientsToUpdate.size());
        System.out.println("=================================");
    }



    public List<Client> getUniqueClients0() {
        List<Client> all = clientRepository.findAll();

        // Ordenar descendente por "recencia"
        all.sort((a, b) -> {
            long aTs = getClientSortTs(a);
            long bTs = getClientSortTs(b);
            return Long.compare(bTs, aTs);
        });



        Map<String, Client> unique = new LinkedHashMap<>();

        for (Client c : all) {
            String key = buildClientIdentityKey(c);
            if (!unique.containsKey(key)) {
                unique.put(key, c); // como ya está ordenado desc, el primero es el más reciente
            }
        }

        return new ArrayList<>(unique.values());
    }


    @Transactional(readOnly = true)
    public List<Client> getUniqueClients() {
        return clientRepository.findUniqueClientsLatestSnapshot();
    }

    @Transactional(readOnly = true)
    public PagedResponse<Client> getUniqueClientsPage(String search, Integer page, Integer size) {
        int safePage = Math.max(page != null ? page : 0, 0);
        int safeSize = Math.min(Math.max(size != null ? size : 20, 1), 100);
        String normalizedSearch = search != null ? search.trim() : "";

        Page<Client> result = clientRepository.findUniqueClientsLatestSnapshotPage(
                normalizedSearch,
                PageRequest.of(safePage, safeSize)
        );

        return new PagedResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast()
        );
    }


    public long getMonthlyServiceCountByDni(String dni, YearMonth ym) {
        if (dni == null || dni.trim().isEmpty()) return 0L;

        ZoneId zone = ZoneId.of("America/Argentina/Buenos_Aires");

        LocalDate start = ym.atDay(1);
        LocalDate end = ym.plusMonths(1).atDay(1);

        Date fromDate = Date.from(start.atStartOfDay(zone).toInstant());
        Date toDate = Date.from(end.atStartOfDay(zone).toInstant());

        long fromMs = fromDate.getTime();
        long toMs = toDate.getTime();

        return clientRepository.countMonthlyServicesByDni(
                dni.trim(),
                fromDate,
                toDate,
                fromMs,
                toMs
        );
    }

    public Map<String, Long> getMonthlyServiceCountsByDnis(List<String> dnis, YearMonth ym) {
        Map<String, Long> result = new LinkedHashMap<>();
        if (dnis == null || dnis.isEmpty()) return result;

        // Normalizar y deduplicar
        List<String> cleanDnis = dnis.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();

        if (cleanDnis.isEmpty()) return result;

        ZoneId zone = ZoneId.of("America/Argentina/Buenos_Aires");
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.plusMonths(1).atDay(1);

        Date fromDate = Date.from(start.atStartOfDay(zone).toInstant());
        Date toDate = Date.from(end.atStartOfDay(zone).toInstant());

        long fromMs = fromDate.getTime();
        long toMs = toDate.getTime();

        // Inicializar en 0 para todos los DNIs pedidos
        for (String dni : cleanDnis) {
            result.put(dni, 0L);
        }

        List<Object[]> byEntry = clientRepository.countMonthlyServicesByDniUsingEntryTimestamp(cleanDnis, fromDate, toDate);
        for (Object[] row : byEntry) {
            String dni = row[0] != null ? row[0].toString() : null;
            long count = row[1] != null ? Long.parseLong(row[1].toString()) : 0L;
            if (dni != null) result.put(dni, result.getOrDefault(dni, 0L) + count);
        }

        List<Object[]> byExit = clientRepository.countMonthlyServicesByDniUsingExitTimestamp(cleanDnis, fromMs, toMs);
        for (Object[] row : byExit) {
            String dni = row[0] != null ? row[0].toString() : null;
            long count = row[1] != null ? Long.parseLong(row[1].toString()) : 0L;
            if (dni != null) result.put(dni, result.getOrDefault(dni, 0L) + count);
        }

        return result;
    }


    private long getClientSortTs(Client c) {
        // prioridad: entryTimestamp, luego exitTimestamp, luego id
        if (c.getEntryTimestamp() != null) return c.getEntryTimestamp().getTime();
        if (c.getExitTimestamp() != null) return c.getExitTimestamp();
        return c.getId() != null ? c.getId() : 0L;
    }

    private String buildClientIdentityKey(Client c) {
        String dni = c.getDni() != null ? c.getDni().trim() : "";
        String phone = c.getPhoneIntl() != null ? c.getPhoneIntl().replaceAll("\\D", "") : "";
        String name = c.getName() != null ? c.getName().trim().toLowerCase() : "";

        if (!dni.isBlank()) return "dni:" + dni;
        if (!phone.isBlank()) return "phone:" + phone;
        return "name:" + name;
    }

    private List<Client> findClientsByIdentity(Client client) {
        String dni = client.getDni() != null ? client.getDni().trim() : "";
        if (!dni.isBlank()) {
            return clientRepository.findAllByDni(dni);
        }

        String phoneDigits = normalizePhoneDigits(client.getPhoneIntl());
        if (phoneDigits.isBlank()) {
            phoneDigits = normalizePhoneDigits(client.getPhoneRaw());
        }
        if (!phoneDigits.isBlank()) {
            return clientRepository.findAllByPhoneDigits(phoneDigits);
        }

        String normalizedName = client.getName() != null ? client.getName().trim() : "";
        if (!normalizedName.isBlank()) {
            return clientRepository.findAllByNameIgnoreCase(normalizedName);
        }

        return List.of(client);
    }

    private void releaseSpacesForClients(List<Client> clientsToDelete) {
        Map<String, Space> spacesToRelease = new LinkedHashMap<>();

        Set<Long> clientIds = clientsToDelete.stream()
                .map(Client::getId)
                .filter(Objects::nonNull)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);

        if (!clientIds.isEmpty()) {
            for (Space space : spaceRepository.findByClientIdIn(clientIds)) {
                spacesToRelease.put(space.getKey(), space);
            }
        }

        for (Client client : clientsToDelete) {
            String spaceKey = client.getSpaceKey();
            if (spaceKey != null && !spaceKey.isBlank()) {
                spaceRepository.findById(spaceKey).ifPresent(space -> spacesToRelease.put(space.getKey(), space));
            }
        }

        if (spacesToRelease.isEmpty()) {
            return;
        }

        for (Space space : spacesToRelease.values()) {
            space.setOccupied(false);
            space.setHold(false);
            space.setClientId(null);
            space.setStartTime(null);
            space.setClient(null);
        }

        spaceRepository.saveAll(spacesToRelease.values());
    }

    private String normalizePhoneDigits(String value) {
        return value != null ? value.replaceAll("\\D", "") : "";
    }

}
