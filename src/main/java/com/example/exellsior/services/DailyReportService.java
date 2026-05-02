package com.example.exellsior.services;

import com.example.exellsior.entity.Client;
import com.example.exellsior.entity.Report;
import com.example.exellsior.entity.Space;
import com.example.exellsior.entity.Subsuelo;
import com.example.exellsior.repository.ClientRepository;
import com.example.exellsior.repository.ReportRepository;
import com.example.exellsior.repository.SpaceRepository;
import com.example.exellsior.repository.SubsueloRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class DailyReportService {

    @Autowired private ReportRepository reportRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private SpaceRepository spaceRepository;
    @Autowired private SubsueloRepository subsueloRepository;
    @Autowired private ClientService clientService;

    private final ObjectMapper mapper = new ObjectMapper();

    public Report upsertDailySnapshotForDay(LocalDate day) {
        return saveDailyReportForDay(day, false);
    }

    public Report finalizeDailyReportForDay(LocalDate day) {
        return saveDailyReportForDay(day, true);
    }

    public void finalizeDailyReportAndResetDay(LocalDate day) {
        Report r = finalizeDailyReportForDay(day);
        log.info("[DAILY-CLOSE] day={} reportId={}", day, r != null ? r.getId() : "null");
        clientService.resetAllDataForDay(day);
    }

    @Scheduled(cron = "0 59 23 * * *", zone = "America/Argentina/Buenos_Aires")
    public void autoDailyCloseEndOfDay() {
        runDailyCloseIfNeeded("SCHEDULED");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void catchUpDailyCloseOnStartup() {
        ZoneId zone = ZoneId.of("America/Argentina/Buenos_Aires");
        LocalDate today = LocalDate.now(zone);
        LocalDate yesterday = today.minusDays(1);
        LocalDate lastClosedDay = getLastClosedDay(zone);
        LocalDate startDay = (lastClosedDay == null) ? yesterday : lastClosedDay.plusDays(1);

        if (startDay.isAfter(yesterday)) {
            log.info("[DAILY-CATCHUP] No hay dias pendientes.");
            return;
        }

        log.info("[DAILY-CATCHUP] startDay={} yesterday={}", startDay, yesterday);

        LocalDate day = startDay;
        while (!day.isAfter(yesterday)) {
            log.info("[DAILY-CATCHUP] Cerrando dia: {}", day);
            try {
                try {
                    finalizeDailyReportForDay(day);
                    log.info("[DAILY-CATCHUP] Reporte OK para {}", day);
                } catch (Exception e) {
                    log.error("[DAILY-CATCHUP] Fallo reporte para {}, continua reset", day, e);
                }
                clientService.resetAllDataForDay(day);
                log.info("[DAILY-CATCHUP] Reset OK para {}", day);
            } catch (Exception e) {
                log.error("[DAILY-CATCHUP] Error en reset para {}", day, e);
                break;
            }
            day = day.plusDays(1);
        }
    }

    private Report saveDailyReportForDay(LocalDate day, boolean dailyFinal) {
        ZoneId zone = ZoneId.of("America/Argentina/Buenos_Aires");
        String periodKey = day.format(DateTimeFormatter.ISO_DATE);

        Date[] range = dayRange(day, zone);
        List<Client> todayClients = clientRepository.findByEntryTimestampBetween(range[0], range[1]);
        List<Report> sameDayReports = reportRepository.findAllByPeriodTypeAndPeriodKey("DAILY", periodKey);

        if (!dailyFinal) {
            Optional<Report> existingFinal = sameDayReports.stream()
                    .filter(r -> Boolean.TRUE.equals(r.getDailyFinal()))
                    .max(Comparator.comparing(Report::getTimestamp, Comparator.nullsLast(String::compareTo)));
            if (existingFinal.isPresent()) {
                log.info("[DAILY-SNAPSHOT] Ya existe reporte final para {}. Se conserva.", periodKey);
                return existingFinal.get();
            }
        }

        List<Map<String, Object>> currentClients = snapshotClients(todayClients);
        List<Map<String, Object>> existingClients = new ArrayList<>();
        for (Report report : sameDayReports) {
            existingClients.addAll(parseJsonSafe(report.getFilteredClients()));
        }

        List<Map<String, Object>> mergedClients = mergeAndDedup(existingClients, currentClients);
        if (sameDayReports.isEmpty() && mergedClients.isEmpty()) {
            log.info("[DAILY-REPORT] No hay datos para el dia {}", periodKey);
            return null;
        }

        List<Space> spaces = spaceRepository.findAll();
        List<Subsuelo> subsuelos = subsueloRepository.findAll();

        int totalSpaces = spaces.size();
        int occupiedSpaces = (int) spaces.stream().filter(Space::isOccupied).count();
        int freeSpaces = totalSpaces - occupiedSpaces;
        int occupancyRate = totalSpaces > 0 ? Math.round((occupiedSpaces * 100f) / totalSpaces) : 0;

        Map<String, Long> paymentAmounts = buildPaymentAmounts(mergedClients);
        long totalCobrado = paymentAmounts.values().stream().mapToLong(Long::longValue).sum();
        Map<String, Integer> timeStats = buildTimeStats(mergedClients);
        List<Map<String, Object>> subsueloStats = buildSubsueloStats(subsuelos, spaces);

        Report report = new Report();
        report.setTimestamp(OffsetDateTime.now(zone).toString());
        report.setPeriodType("DAILY");
        report.setPeriodKey(periodKey);
        report.setDailyFinal(dailyFinal);
        report.setTotalSpaces(totalSpaces);
        report.setOccupiedSpaces(occupiedSpaces);
        report.setFreeSpaces(freeSpaces);
        report.setOccupancyRate(occupancyRate);
        report.setTotalCobrado(totalCobrado);

        try {
            report.setSubsueloStats(mapper.writeValueAsString(subsueloStats));
            report.setFilteredClients(mapper.writeValueAsString(mergedClients));
            report.setPaymentAmounts(mapper.writeValueAsString(paymentAmounts));
            report.setTimeStats(mapper.writeValueAsString(timeStats));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializando reporte diario", e);
        }

        List<Report> reportsToReplace = dailyFinal
                ? sameDayReports
                : sameDayReports.stream().filter(r -> !Boolean.TRUE.equals(r.getDailyFinal())).toList();
        if (!reportsToReplace.isEmpty()) {
            reportRepository.deleteAll(reportsToReplace);
        }

        Report saved = reportRepository.save(report);
        log.info("[DAILY-REPORT] Guardado. day={} final={} reportId={} clientes={}",
                periodKey, dailyFinal, saved.getId(), mergedClients.size());
        return saved;
    }

    private void runDailyCloseIfNeeded(String source) {
        ZoneId zone = ZoneId.of("America/Argentina/Buenos_Aires");
        LocalDate today = LocalDate.now(zone);
        long todayStartMs = today.atStartOfDay(zone).toInstant().toEpochMilli();
        boolean staleOccupied = spaceRepository.existsByOccupiedTrueAndStartTimeLessThan(todayStartMs);

        if ("STARTUP".equals(source) && !staleOccupied) {
            log.info("[DAILY-CATCHUP] No hay espacios pendientes.");
            return;
        }

        LocalDate targetDay = "STARTUP".equals(source) ? today.minusDays(1) : today;
        log.info("[DAILY-CLOSE] source={} targetDay={} staleOccupied={}", source, targetDay, staleOccupied);

        try {
            try {
                finalizeDailyReportForDay(targetDay);
                log.info("[DAILY-CLOSE] Reporte OK para {}", targetDay);
            } catch (Exception e) {
                log.error("[DAILY-CLOSE] Fallo reporte, continua reset", e);
            }
            clientService.resetAllDataForDay(targetDay);
            log.info("[DAILY-CLOSE] Reset OK para {}", targetDay);
        } catch (Exception e) {
            log.error("[DAILY-CLOSE] Error durante reset", e);
        }
    }

    private LocalDate getLastClosedDay(ZoneId zone) {
        Long ms = clientRepository.findMaxLastDayClosed();
        if (ms == null || ms <= 0L) return null;
        return new Date(ms).toInstant().atZone(zone).toLocalDate();
    }

    private Date[] dayRange(LocalDate day, ZoneId zone) {
        return new Date[]{
                Date.from(day.atStartOfDay(zone).toInstant()),
                Date.from(day.plusDays(1).atStartOfDay(zone).toInstant())
        };
    }

    private List<Map<String, Object>> snapshotClients(List<Client> clients) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Client c : clients) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", c.getId());
            row.put("code", c.getCode());
            row.put("name", c.getName());
            row.put("dni", c.getDni());
            row.put("phoneIntl", c.getPhoneIntl());
            row.put("phoneRaw", c.getPhoneRaw());
            row.put("plate", c.getPlate());
            row.put("notes", c.getNotes());
            row.put("spaceKey", c.getSpaceKey());
            row.put("vehicle", c.getVehicle());
            row.put("category", c.getCategory());
            row.put("price", c.getPrice());
            row.put("paymentMethod", c.getPaymentMethod());
            row.put("clover", c.getClover());
            row.put("entryTimestamp", c.getEntryTimestamp() != null ? c.getEntryTimestamp().getTime() : null);
            row.put("exitTimestamp", c.getExitTimestamp());
            row.put("spaceDisplayName", c.getSpaceKey() != null ? c.getSpaceKey() : "-");
            result.add(row);
        }
        return result;
    }

    private List<Map<String, Object>> parseJsonSafe(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return mapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> mergeAndDedup(List<Map<String, Object>> existing, List<Map<String, Object>> current) {
        List<Map<String, Object>> merged = new ArrayList<>();
        if (existing != null) merged.addAll(existing);
        if (current != null) merged.addAll(current);

        Map<String, Map<String, Object>> dedup = new LinkedHashMap<>();
        for (Map<String, Object> client : merged) {
            String key = buildClientSnapshotKey(client);
            Map<String, Object> previous = dedup.get(key);
            dedup.put(key, previous == null ? client : mergeClientSnapshots(previous, client));
        }
        List<Map<String, Object>> result = new ArrayList<>(dedup.values());
        result.sort((a, b) -> Long.compare(
                extractComparableTimestamp(b),
                extractComparableTimestamp(a)
        ));
        return result;
    }

    private String buildClientSnapshotKey(Map<String, Object> client) {
        Long entryTs = normalizeEpoch(client.get("entryTimestamp"));
        String code = normalizeText(client.get("code"));
        String dni = normalizeText(client.get("dni"));
        String phone = normalizeDigits(client.get("phoneIntl"));
        String name = normalizeText(client.get("name"));
        String vehicle = normalizeText(client.get("vehicle"));
        String plate = normalizeText(client.get("plate"));
        String id = normalizeText(client.get("id"));

        if (code != null || entryTs != null) {
            return "code:" + (code != null ? code : "x") + "|entry:" + (entryTs != null ? entryTs : "x");
        }

        if (dni != null || phone != null || name != null) {
            return "fallback:" +
                    (dni != null ? dni : "x") + "|" +
                    (phone != null ? phone : "x") + "|" +
                    (name != null ? name : "x") + "|" +
                    (entryTs != null ? entryTs : "x") + "|" +
                    (vehicle != null ? vehicle : "x") + "|" +
                    (plate != null ? plate : "x");
        }

        return "id:" + (id != null ? id : "x") + "|entry:" + (entryTs != null ? entryTs : "x");
    }

    private Map<String, Object> mergeClientSnapshots(Map<String, Object> previous, Map<String, Object> incoming) {
        Map<String, Object> merged = new LinkedHashMap<>(previous);
        for (Map.Entry<String, Object> entry : incoming.entrySet()) {
            Object value = entry.getValue();
            if (isMeaningfulValue(value)) {
                merged.put(entry.getKey(), value);
            }
        }

        Long previousExit = normalizeEpoch(previous.get("exitTimestamp"));
        Long incomingExit = normalizeEpoch(incoming.get("exitTimestamp"));
        if (previousExit != null || incomingExit != null) {
            merged.put("exitTimestamp", incomingExit != null ? incomingExit : previousExit);
        }

        Long previousEntry = normalizeEpoch(previous.get("entryTimestamp"));
        Long incomingEntry = normalizeEpoch(incoming.get("entryTimestamp"));
        if (previousEntry != null || incomingEntry != null) {
            merged.put("entryTimestamp", incomingEntry != null ? incomingEntry : previousEntry);
        }

        return merged;
    }

    private long extractComparableTimestamp(Map<String, Object> client) {
        Long entry = normalizeEpoch(client.get("entryTimestamp"));
        if (entry != null) return entry;
        Long exit = normalizeEpoch(client.get("exitTimestamp"));
        return exit != null ? exit : 0L;
    }

    private Long normalizeEpoch(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof Date date) {
            return date.getTime();
        }

        String text = String.valueOf(value).trim();
        if (text.isEmpty() || "null".equalsIgnoreCase(text) || "x".equalsIgnoreCase(text)) {
            return null;
        }

        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ignored) {
        }

        try {
            return Date.from(OffsetDateTime.parse(text).toInstant()).getTime();
        } catch (Exception ignored) {
        }

        try {
            return new Date(text).getTime();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String normalizeText(Object value) {
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        if (text.isEmpty() || "null".equalsIgnoreCase(text) || "x".equalsIgnoreCase(text)) {
            return null;
        }
        return text;
    }

    private String normalizeDigits(Object value) {
        String text = normalizeText(value);
        if (text == null) return null;
        String digits = text.replaceAll("\\D+", "");
        return digits.isEmpty() ? null : digits;
    }

    private boolean isMeaningfulValue(Object value) {
        if (value == null) return false;
        if (value instanceof String text) {
            return !text.trim().isEmpty() && !"null".equalsIgnoreCase(text.trim());
        }
        return true;
    }

    private Map<String, Long> buildPaymentAmounts(List<Map<String, Object>> clients) {
        Map<String, Long> paymentAmounts = new LinkedHashMap<>();
        for (String key : List.of("efectivo", "credito", "prepago", "qr", "debito", "scaneo", "S/Cargo", "otros")) {
            paymentAmounts.put(key, 0L);
        }

        for (Map<String, Object> client : clients) {
            String method = String.valueOf(client.getOrDefault("paymentMethod", "otros"));
            long amount = 0L;
            try {
                amount = Long.parseLong(String.valueOf(client.getOrDefault("price", 0)));
            } catch (Exception ignored) {}

            String lower = method.toLowerCase();
            String key = switch (lower) {
                case "efectivo" -> "efectivo";
                case "credito" -> "credito";
                case "prepago" -> "prepago";
                case "qr" -> "qr";
                case "debito" -> "debito";
                case "scaneo" -> "scaneo";
                default -> "S/Cargo".equals(method) ? "S/Cargo" : "otros";
            };

            paymentAmounts.put(key, paymentAmounts.get(key) + amount);
        }

        return paymentAmounts;
    }

    private Map<String, Integer> buildTimeStats(List<Map<String, Object>> clients) {
        Map<String, Integer> stats = new LinkedHashMap<>();
        stats.put("under1h", 0);
        stats.put("between1h3h", 0);
        stats.put("over3h", 0);

        long now = System.currentTimeMillis();
        for (Map<String, Object> client : clients) {
            Object entryObj = client.get("entryTimestamp");
            if (entryObj == null) continue;

            try {
                long entryTs = Long.parseLong(String.valueOf(entryObj));
                double hours = (now - entryTs) / 3600000.0;
                if (hours < 1) stats.merge("under1h", 1, Integer::sum);
                else if (hours <= 3) stats.merge("between1h3h", 1, Integer::sum);
                else stats.merge("over3h", 1, Integer::sum);
            } catch (Exception ignored) {}
        }

        return stats;
    }

    private List<Map<String, Object>> buildSubsueloStats(List<Subsuelo> subsuelos, List<Space> spaces) {
        Map<String, int[]> accumulator = new LinkedHashMap<>();
        for (Subsuelo subsuelo : subsuelos) {
            accumulator.put(subsuelo.getId(), new int[]{0, 0});
        }

        for (Space space : spaces) {
            String subsueloId = space.getSubsueloId();
            if (subsueloId == null) continue;
            int[] counts = accumulator.computeIfAbsent(subsueloId, ignored -> new int[]{0, 0});
            counts[0] += 1;
            if (space.isOccupied()) {
                counts[1] += 1;
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Subsuelo subsuelo : subsuelos) {
            int[] counts = accumulator.getOrDefault(subsuelo.getId(), new int[]{0, 0});
            int total = counts[0];
            int occupied = counts[1];
            int free = total - occupied;
            int occupancyRate = total > 0 ? Math.round((occupied * 100f) / total) : 0;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", subsuelo.getId());
            row.put("label", subsuelo.getLabel());
            row.put("total", total);
            row.put("occupied", occupied);
            row.put("free", free);
            row.put("occupancyRate", occupancyRate);
            result.add(row);
        }
        return result;
    }
}
