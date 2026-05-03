package com.example.exellsior.services;

import com.example.exellsior.entity.Client;
import com.example.exellsior.entity.Report;
import com.example.exellsior.entity.ServiceHistory;
import com.example.exellsior.repository.ReportRepository;
import com.example.exellsior.repository.ServiceHistoryRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class ServiceHistoryService {

    private static final Logger log = LoggerFactory.getLogger(ServiceHistoryService.class);

    @Autowired
    private ServiceHistoryRepository serviceHistoryRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private OperationalTimeService operationalTimeService;

    private final ObjectMapper mapper = new ObjectMapper();

    @Transactional
    public ServiceHistory upsertFromClient(Client client, Long exitTimestampMs, LocalDate serviceDay, String archivedBy) {
        if (client == null || client.getName() == null || client.getName().trim().isEmpty()) {
            return null;
        }

        Long entryTimestampMs = client.getEntryTimestamp() != null ? client.getEntryTimestamp().getTime() : null;
        Long effectiveExitTimestamp = exitTimestampMs != null
                ? exitTimestampMs
                : (client.getExitTimestamp() != null ? client.getExitTimestamp() : System.currentTimeMillis());

        LocalDate effectiveServiceDay = serviceDay != null
                ? serviceDay
                : Instant.ofEpochMilli(effectiveExitTimestamp).atZone(operationalTimeService.getBusinessZone()).toLocalDate();

        String serviceKey = buildServiceKey(client, entryTimestampMs, effectiveServiceDay);

        ServiceHistory history = serviceHistoryRepository.findByServiceKey(serviceKey)
                .orElseGet(ServiceHistory::new);

        history.setServiceKey(serviceKey);
        history.setSourceClientId(client.getId() != null ? Long.valueOf(client.getId().toString()) : null);
        history.setCode(clean(client.getCode()));
        history.setName(clean(client.getName()));
        history.setDni(clean(client.getDni()));
        history.setPhoneIntl(clean(client.getPhoneIntl()));
        history.setPhoneRaw(clean(client.getPhoneRaw()));
        history.setPlate(clean(client.getPlate()));
        history.setNotes(clean(client.getNotes()));
        history.setSpaceKey(clean(client.getSpaceKey()));
        history.setVehicle(clean(client.getVehicle()));
        history.setCategory(clean(client.getCategory()));
        history.setPrice(client.getPrice());
        history.setPaymentMethod(clean(client.getPaymentMethod()));
        history.setClover(client.getClover());
        history.setEntryTimestamp(entryTimestampMs);
        history.setExitTimestamp(effectiveExitTimestamp);
        history.setServiceDate(effectiveServiceDay);
        history.setArchivedAt(System.currentTimeMillis());
        history.setArchivedBy(archivedBy != null ? archivedBy : "SYSTEM");

        return serviceHistoryRepository.save(history);
    }

    @Transactional(readOnly = true)
    public List<ServiceHistory> getByDateRange(LocalDate from, LocalDate to) {
        return serviceHistoryRepository.findByServiceDateBetweenOrderByServiceDateDescExitTimestampDesc(from, to);
    }

    @Transactional
    public void deleteBySourceClientIds(Collection<Long> clientIds) {
        if (clientIds == null || clientIds.isEmpty()) return;
        serviceHistoryRepository.deleteBySourceClientIdIn(clientIds);
    }

    @Transactional
    public void deleteAll() {
        serviceHistoryRepository.deleteAll();
        log.info("[SERVICE-HISTORY-RESET] Historico eliminado completamente.");
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void backfillFromDailyReportsOnStartup() {
        List<Report> dailyReports = reportRepository.findByPeriodTypeAndPeriodKeyStartingWith("DAILY", "");
        Map<LocalDate, Report> selectedReportsByDay = new LinkedHashMap<>();
        LocalDate today = LocalDate.now(operationalTimeService.getBusinessZone());

        for (Report report : dailyReports) {
            if (report.getPeriodKey() == null || report.getPeriodKey().isBlank()) {
                continue;
            }

            LocalDate serviceDay;
            try {
                serviceDay = LocalDate.parse(report.getPeriodKey());
            } catch (Exception ignored) {
                continue;
            }

            if (serviceDay.isAfter(today) || serviceDay.equals(today)) {
                continue;
            }

            Report previous = selectedReportsByDay.get(serviceDay);
            if (previous == null) {
                selectedReportsByDay.put(serviceDay, report);
                continue;
            }

            boolean incomingIsFinal = Boolean.TRUE.equals(report.getDailyFinal());
            boolean previousIsFinal = Boolean.TRUE.equals(previous.getDailyFinal());
            if (incomingIsFinal && !previousIsFinal) {
                selectedReportsByDay.put(serviceDay, report);
                continue;
            }

            if (incomingIsFinal == previousIsFinal) {
                String previousTs = previous.getTimestamp() != null ? previous.getTimestamp() : "";
                String incomingTs = report.getTimestamp() != null ? report.getTimestamp() : "";
                if (incomingTs.compareTo(previousTs) > 0) {
                    selectedReportsByDay.put(serviceDay, report);
                }
            }
        }

        if (selectedReportsByDay.isEmpty()) {
            log.info("[SERVICE-HISTORY-BACKFILL] No hay reportes diarios para reconstruir.");
            return;
        }

        Set<LocalDate> existingDays = serviceHistoryRepository.findExistingServiceDates(selectedReportsByDay.keySet());
        int missingDays = 0;

        for (Map.Entry<LocalDate, Report> entry : selectedReportsByDay.entrySet()) {
            LocalDate serviceDay = entry.getKey();
            if (existingDays.contains(serviceDay)) {
                continue;
            }

            missingDays++;
            Report report = entry.getValue();
            for (Map<String, Object> row : parseClients(report.getFilteredClients())) {
                upsertFromSnapshot(row, serviceDay, "REPORT_BACKFILL");
            }
        }

        if (missingDays == 0) {
            log.info("[SERVICE-HISTORY-BACKFILL] Historico ya consistente. Dias evaluados: {}", selectedReportsByDay.size());
            return;
        }

        log.info("[SERVICE-HISTORY-BACKFILL] Reconstruidos {} dia(s) faltantes de {} evaluados.", missingDays, selectedReportsByDay.size());
    }

    @Transactional
    public ServiceHistory upsertFromSnapshot(Map<String, Object> snapshot, LocalDate serviceDay, String archivedBy) {
        if (snapshot == null || serviceDay == null) {
            return null;
        }

        String name = clean(asString(snapshot.get("name")));
        if (name == null) {
            return null;
        }

        Long sourceClientId = asLong(snapshot.get("id"));
        Long entryTimestampMs = asLong(snapshot.get("entryTimestamp"));
        Long exitTimestampMs = asLong(snapshot.get("exitTimestamp"));
        if (exitTimestampMs == null) {
            exitTimestampMs = entryTimestampMs != null ? entryTimestampMs : System.currentTimeMillis();
        }

        String serviceKey = buildServiceKey(
                firstNonBlank(
                        clean(asString(snapshot.get("code"))),
                        asString(snapshot.get("dni")),
                        asString(snapshot.get("phoneIntl")),
                        asString(snapshot.get("phoneRaw")),
                        name,
                        sourceClientId != null ? sourceClientId.toString() : null,
                        "anon"
                ),
                entryTimestampMs,
                serviceDay,
                asString(snapshot.get("spaceKey")),
                asString(snapshot.get("vehicle")),
                asString(snapshot.get("plate"))
        );

        ServiceHistory history = serviceHistoryRepository.findByServiceKey(serviceKey)
                .orElseGet(ServiceHistory::new);

        history.setServiceKey(serviceKey);
        history.setSourceClientId(sourceClientId);
        history.setCode(clean(asString(snapshot.get("code"))));
        history.setName(name);
        history.setDni(clean(asString(snapshot.get("dni"))));
        history.setPhoneIntl(clean(asString(snapshot.get("phoneIntl"))));
        history.setPhoneRaw(clean(asString(snapshot.get("phoneRaw"))));
        history.setPlate(clean(asString(snapshot.get("plate"))));
        history.setNotes(clean(asString(snapshot.get("notes"))));
        history.setSpaceKey(clean(asString(snapshot.get("spaceKey"))));
        history.setVehicle(clean(asString(snapshot.get("vehicle"))));
        history.setCategory(clean(asString(snapshot.get("category"))));
        history.setPrice(asInteger(snapshot.get("price")));
        history.setPaymentMethod(clean(asString(snapshot.get("paymentMethod"))));
        history.setClover(asInteger(snapshot.get("clover")));
        history.setEntryTimestamp(entryTimestampMs);
        history.setExitTimestamp(exitTimestampMs);
        history.setServiceDate(serviceDay);
        history.setArchivedAt(System.currentTimeMillis());
        history.setArchivedBy(archivedBy != null ? archivedBy : "SYSTEM");

        return serviceHistoryRepository.save(history);
    }

    private String buildServiceKey(Client client, Long entryTimestampMs, LocalDate serviceDay) {
        String clientIdentity = firstNonBlank(
                client.getCode(),
                client.getDni(),
                client.getPhoneIntl(),
                client.getPhoneRaw(),
                client.getName(),
                client.getId() != null ? client.getId().toString() : null,
                "anon"
        );

        return buildServiceKey(
                clientIdentity,
                entryTimestampMs,
                serviceDay,
                client.getSpaceKey(),
                client.getVehicle(),
                client.getPlate()
        );
    }

    private String buildServiceKey(String clientIdentity, Long entryTimestampMs, LocalDate serviceDay,
                                   String spaceKey, String vehicle, String plate) {
        return String.join("|",
                normalize(clientIdentity),
                String.valueOf(entryTimestampMs != null ? entryTimestampMs : 0L),
                serviceDay != null ? serviceDay.toString() : "no-day",
                normalize(spaceKey),
                normalize(vehicle),
                normalize(plate)
        );
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String clean = clean(value);
            if (clean != null) {
                return clean;
            }
        }
        return null;
    }

    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalize(String value) {
        return Objects.toString(clean(value), "").trim().toLowerCase();
    }

    private List<Map<String, Object>> parseClients(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return mapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }

    private Long asLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(value.toString());
        } catch (Exception ignored) {
            return null;
        }
    }

    private Integer asInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.valueOf(value.toString());
        } catch (Exception ignored) {
            return null;
        }
    }
}
