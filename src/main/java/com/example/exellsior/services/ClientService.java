package com.example.exellsior.services;

import com.example.exellsior.dto.PagedResponse;
import com.example.exellsior.entity.Client;
import com.example.exellsior.entity.ClientVehicle;
import com.example.exellsior.entity.Space;
import com.example.exellsior.entity.VehicleType;
import com.example.exellsior.repository.ClientRepository;
import com.example.exellsior.repository.ClientVehicleRepository;
import com.example.exellsior.repository.SpaceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.Date;

@Slf4j
@Service
public class ClientService {
    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private ClientVehicleRepository clientVehicleRepository;


    @Autowired
    private VehicleTypeService vehicleTypeService;

    @Autowired
    private ServiceHistoryService serviceHistoryService;

    @Autowired
    private OperationalTimeService operationalTimeService;

    @Transactional(readOnly = true)
    public List<Client> getAllClients() {
        return clientRepository.findAll();
    }

    @Transactional(readOnly = true)
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
        Client saved = clientRepository.save(client);
        initializeClientAssociations(saved);
        return saved;
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

        Client saved = clientRepository.save(client);
        initializeClientAssociations(saved);
        return saved;
    }


    @Transactional
    public void updateVehiclesForAllByDni(String dni, List<Map<String, Object>> vehiclesPayload) {
        if (dni == null || dni.isBlank()) return;
        List<Client> clients = clientRepository.findAllByDni(dni.trim());
        if (clients == null || clients.isEmpty()) return;

        List<ClientVehicle> incoming = new ArrayList<>();
        if (vehiclesPayload != null) {
            Client ref = clients.get(0);
            for (Object item : vehiclesPayload) {
                if (!(item instanceof Map)) continue;
                Map<?, ?> m = (Map<?, ?>) item;
                Object vtObj = m.get("vehicleType");
                if (!(vtObj instanceof Map)) continue;
                Object idObj = ((Map<?, ?>) vtObj).get("id");
                if (idObj == null) continue;
                ClientVehicle cv = new ClientVehicle();
                VehicleType vt = vehicleTypeService.getById(Long.valueOf(idObj.toString()));
                cv.setVehicleType(vt);
                cv.setPlate(m.get("plate") != null ? m.get("plate").toString() : null);
                cv.setNotes(m.get("notes") != null ? m.get("notes").toString() : null);
                cv.setClient(ref);
                incoming.add(cv);
            }
        }

        if (incoming.size() > 4) {
            throw new RuntimeException("Solo se permiten hasta 4 vehículos por cliente");
        }

        for (Client client : clients) {
            List<ClientVehicle> resolved = new ArrayList<>();
            for (ClientVehicle src : incoming) {
                ClientVehicle cv = new ClientVehicle();
                cv.setClient(client);
                cv.setVehicleType(src.getVehicleType());
                cv.setPlate(src.getPlate());
                cv.setNotes(src.getNotes());
                resolved.add(cv);
            }
            replaceClientVehicles(client, resolved);
            clientRepository.save(client);
        }
    }


    @Transactional
    public void deleteClient(Long id) {
        Client client = getById(id);
        List<Client> clientsToDelete = findClientsByIdentity(client);

        if (clientsToDelete.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado");
        }

        Set<Long> clientIdsToDelete = clientsToDelete.stream()
                .map(Client::getId)
                .filter(Objects::nonNull)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);

        releaseSpacesForClients(clientsToDelete);

        if (!clientIdsToDelete.isEmpty()) {
            clientVehicleRepository.deleteByClientIdIn(clientIdsToDelete);
            serviceHistoryService.deleteBySourceClientIds(clientIdsToDelete);
        }

        clientsToDelete.forEach(c -> {
            if (c.getClientVehicles() != null) {
                c.getClientVehicles().clear();
            }
        });

        clientRepository.deleteAll(clientsToDelete);
    }

    @Transactional
    public void deleteService(Long id) {
        Client client = getById(id);

        if (client.getSpaceKey() != null && !client.getSpaceKey().isBlank()) {
            Space space = spaceRepository.findById(client.getSpaceKey()).orElse(null);
            if (space != null) {
                Long currentClientId = space.getClientId();
                if (currentClientId != null && Objects.equals(currentClientId, client.getId())) {
                    space.setOccupied(false);
                    space.setHold(false);
                    space.setClientId(null);
                    space.setStartTime(null);
                    space.setClient(null);
                    spaceRepository.save(space);
                }
            }
        }

        if (client.getClientVehicles() != null) {
            client.getClientVehicles().clear();
        }

        if (client.getId() != null) {
            clientVehicleRepository.deleteByClientIdIn(Set.of(client.getId()));
            serviceHistoryService.deleteBySourceClientIds(Set.of(client.getId()));
        }

        clientRepository.delete(client);
    }




    @Transactional(readOnly = true)
    public List<Client> getByDateRange(LocalDate from, LocalDate to) {
        ZoneId zone = operationalTimeService.getBusinessZone();
        Date dateFrom = Date.from(from.atStartOfDay(zone).toInstant());
        Date dateTo = Date.from(to.plusDays(1).atStartOfDay(zone).toInstant());
        return clientRepository.findByEntryTimestampBetween(dateFrom, dateTo);
    }

    @Transactional(readOnly = true)
    public Client getByDni(String dni) {
        return clientRepository.findFirstByDniOrderByIdDesc(dni).orElse(null);
    }

    @Transactional
    public void resetAllDataForDay(LocalDate operationalDay) {
        ZoneId zone = operationalTimeService.getBusinessZone();
        long nowMs = System.currentTimeMillis();
        long closeDayMarkerMs = operationalDay.atStartOfDay(zone).toInstant().toEpochMilli();

        List<Space> spacesToReset = spaceRepository.findByOccupiedTrueOrHoldTrueOrClientIdIsNotNullOrStartTimeIsNotNull();
        Set<Long> affectedClientIds = new HashSet<>();

        for (Space space : spacesToReset) {
            if (space.getClientId() != null) {
                affectedClientIds.add(space.getClientId());
            }

            space.setOccupied(false);
            space.setHold(false);
            space.setClientId(null);
            space.setStartTime(null);
            space.setClient(null);
        }

        if (!spacesToReset.isEmpty()) {
            spaceRepository.saveAll(spacesToReset);
        }

        List<Client> clientsToUpdate = new ArrayList<>();
        List<Client> candidateClients = affectedClientIds.isEmpty()
                ? clientRepository.findBySpaceKeyIsNotNull()
                : clientRepository.findBySpaceKeyIsNotNullOrIdIn(affectedClientIds);

        for (Client client : candidateClients) {
            boolean hasSpaceKey = client.getSpaceKey() != null && !client.getSpaceKey().isBlank();
            boolean wasLinkedBySpace = client.getId() != null && affectedClientIds.contains(client.getId());

            if (!hasSpaceKey && !wasLinkedBySpace) {
                continue;
            }

            if (client.getExitTimestamp() == null) {
                client.setExitTimestamp(nowMs);
            }

            serviceHistoryService.upsertFromClient(client, client.getExitTimestamp(), operationalDay, "DAY_CLOSE");

            client.setSpaceKey(null);
            client.setEntryTimestamp(null);
            client.setLastDayClosed(closeDayMarkerMs);

            clientsToUpdate.add(client);
        }

        if (!clientsToUpdate.isEmpty()) {
            clientRepository.saveAll(clientsToUpdate);
        }
    }

    @Transactional(readOnly = true)
    public List<Client> getReservationsByDni(String dni) {
        if (dni == null || dni.trim().isEmpty()) return List.of();
        List<Client> clients = clientRepository.findByDniOrderByIdDesc(dni.trim());
        clients.forEach(this::initializeClientAssociations);
        return clients;
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

        initializeClientAssociations(client);
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

            serviceHistoryService.upsertFromClient(client, client.getExitTimestamp(), null, "MANUAL_RELEASE");

            // NO limpiar otros datos (price, vehicle, etc.)
            clientRepository.save(client);
        }

        log.info("Espacio liberado. Cliente {} marcado con exitTimestamp", clientId);
    }



 

    @Transactional
    public void resetAllData() {
        log.info("=== INICIO DEL CIERRE DEL DÍA ===");
        long nowMs = System.currentTimeMillis();
        log.info("Fecha/Hora: {}", new Date(nowMs));

        // 1) Cargar solo espacios con estado operativo pendiente
        List<Space> spacesToReset = spaceRepository.findByOccupiedTrueOrHoldTrueOrClientIdIsNotNullOrStartTimeIsNotNull();

        // 2) Capturar clientIds vinculados a espacios antes de limpiar (para no depender solo de spaceKey)
        Set<Long> affectedClientIds = new HashSet<>();
        int spacesReset = spacesToReset.size();

        log.info("Liberando espacios...");
        for (Space space : spacesToReset) {
            if (space.getClientId() != null) {
                affectedClientIds.add(space.getClientId());
            }

            log.debug("Reseteando espacio: {} | occupied={} | hold={} | clientId={} | startTime={}",
                    space.getKey(), space.isOccupied(), space.isHold(), space.getClientId(), space.getStartTime());

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
        log.info("Buscando clientes para resetear...");

        List<Client> candidateClients = affectedClientIds.isEmpty()
                ? clientRepository.findBySpaceKeyIsNotNull()
                : clientRepository.findBySpaceKeyIsNotNullOrIdIn(affectedClientIds);

        for (Client client : candidateClients) {
            boolean hasSpaceKey = client.getSpaceKey() != null && !client.getSpaceKey().isBlank();
            boolean wasLinkedBySpace = client.getId() != null && affectedClientIds.contains(client.getId());

            if (hasSpaceKey || wasLinkedBySpace) {
                log.debug("Reseteando cliente ID {} | spaceKey={} | entryTimestamp={} | exitTimestamp={}",
                        client.getId(), client.getSpaceKey(), client.getEntryTimestamp(), client.getExitTimestamp());

                if (client.getExitTimestamp() == null) {
                    client.setExitTimestamp(nowMs);
                    log.debug("exitTimestamp forzado: {}", new Date(nowMs));
                }

                serviceHistoryService.upsertFromClient(client, client.getExitTimestamp(), null, "DAY_CLOSE");

                client.setSpaceKey(null);
                client.setEntryTimestamp(null);

                // Marca de último cierre diario aplicado
                client.setLastDayClosed(nowMs);

                clientsToUpdate.add(client);
            }
        }

        if (!clientsToUpdate.isEmpty()) {
            log.info("Guardando {} clientes reseteados...", clientsToUpdate.size());
            clientRepository.saveAll(clientsToUpdate);
        } else {
            log.info("No hubo clientes para resetear.");
        }

        log.info("=== CIERRE DEL DÍA FINALIZADO — espacios={} reseteados={} clientes={}",
                spacesToReset.size(), spacesReset, clientsToUpdate.size());
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


    @Transactional(readOnly = true)
    public long getMonthlyServiceCountByDni(String dni, YearMonth ym) {
        if (dni == null || dni.trim().isEmpty()) return 0L;

        ZoneId zone = operationalTimeService.getBusinessZone();

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

    @Transactional(readOnly = true)
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

        ZoneId zone = operationalTimeService.getBusinessZone();
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

    private void initializeClientAssociations(Client client) {
        if (client == null) {
            return;
        }

        List<ClientVehicle> vehicles = client.getClientVehicles();
        if (vehicles == null) {
            client.setClientVehicles(new ArrayList<>());
            return;
        }

        vehicles.size();
        for (ClientVehicle vehicle : vehicles) {
            if (vehicle == null) {
                continue;
            }

            VehicleType vehicleType = vehicle.getVehicleType();
            if (vehicleType != null) {
                vehicleType.getId();
                vehicleType.getModel();
                vehicleType.getCategory();
                vehicleType.getPrice();
            }
        }
    }

}
