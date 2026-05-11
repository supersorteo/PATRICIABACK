package com.example.exellsior.services;

import com.example.exellsior.entity.Client;
import com.example.exellsior.entity.Report;
import com.example.exellsior.entity.ServiceHistory;
import com.example.exellsior.entity.Space;
import com.example.exellsior.entity.Subsuelo;
import com.example.exellsior.repository.ClientRepository;
import com.example.exellsior.repository.ReportRepository;
import com.example.exellsior.repository.SpaceRepository;
import com.example.exellsior.repository.SubsueloRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DailyReportService {

    @Autowired private ReportRepository reportRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private SpaceRepository spaceRepository;
    @Autowired private SubsueloRepository subsueloRepository;
    @Autowired private ClientService clientService;
    @Autowired private MonthlyReportService monthlyReportService;
    @Autowired private ServiceHistoryService serviceHistoryService;
    @Autowired private OperationalTimeService operationalTimeService;

    private final ObjectMapper mapper = new ObjectMapper();

    // ─── PUBLIC API ───────────────────────────────────────────────────────────

    /** Called by ReportScheduleService at configured time → SCHEDULED snapshot */
    public Report upsertDailySnapshotForDay(LocalDate day) {
        return saveDailySnapshotReport(day, "SCHEDULED");
    }

    /** Day-close: capture space stats → reset → build DAY_CLOSE from ServiceHistory */
    public void finalizeDailyReportAndResetDay(LocalDate day) {
        SpaceStats spaceStats = captureSpaceStats();
        log.info("[DAY-CLOSE] day={} Iniciando reset. Espacios ocupados antes: {}", day, spaceStats.occupiedSpaces);
        clientService.resetAllDataForDay(day);
        log.info("[DAY-CLOSE] day={} Reset completo. Generando DAY_CLOSE desde ServiceHistory", day);
        Report r = generateDayCloseFromServiceHistory(day, spaceStats);
        operationalTimeService.markLastCloseDay(day);
        generateMonthlyIfMonthEnded(day);
        log.info("[DAY-CLOSE] day={} reportId={}", day, r != null ? r.getId() : "null");
    }

    /** Used by catch-up on startup: clients already reset, read ServiceHistory */
    public Report finalizeDailyReportForDay(LocalDate day) {
        Report report = generateDayCloseFromServiceHistory(day, null);
        operationalTimeService.markLastCloseDay(day);
        generateMonthlyIfMonthEnded(day);
        return report;
    }

    // ─── SCHEDULED ────────────────────────────────────────────────────────────

    @Scheduled(cron = "0 * * * * *")
    public void autoDailyCloseEndOfDay() {
        ZoneId zone = operationalTimeService.getBusinessZone();
        LocalDate today = LocalDate.now(zone);
        LocalTime nowTime = LocalTime.now(zone).withSecond(0).withNano(0);
        LocalTime closeTime = operationalTimeService.getDailyCloseTime();

        if (!nowTime.equals(closeTime)) {
            return;
        }

        LocalDate settingsLastCloseDay = operationalTimeService.getLastCloseDay();
        LocalDate lastClosedReportDay = getLastClosedDay();
        LocalDate effectiveLastCloseDay = maxDay(settingsLastCloseDay, lastClosedReportDay);

        log.info("[DAILY-CLOSE] Hora de cierre alcanzada. today={} nowTime={} closeTime={} zone={} settingsLastCloseDay={} lastClosedReportDay={}",
                today, nowTime, closeTime, zone, settingsLastCloseDay, lastClosedReportDay);

        if (today.equals(effectiveLastCloseDay)) {
            log.info("[DAILY-CLOSE] Cierre omitido para {} porque ya existe un cierre registrado hoy. effectiveLastCloseDay={}",
                    today, effectiveLastCloseDay);
            return;
        }

        closeOperationalDay(today, true, "SCHEDULED");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void catchUpDailyCloseOnStartup() {
        operationalTimeService.fixTimezoneIfNeeded();
        ZoneId zone = operationalTimeService.getBusinessZone();
        LocalDate today = LocalDate.now(zone);
        LocalDate yesterday = today.minusDays(1);
        LocalDate lastClosedDay = maxDay(operationalTimeService.getLastCloseDay(), getLastClosedDay());
        LocalDate startDay = (lastClosedDay == null) ? yesterday : lastClosedDay.plusDays(1);

        if (startDay.isAfter(yesterday)) {
            log.info("[DAILY-CATCHUP] No hay dias pendientes.");
            return;
        }

        log.info("[DAILY-CATCHUP] startDay={} yesterday={}", startDay, yesterday);

        LocalDate day = startDay;
        while (!day.isAfter(yesterday)) {
            boolean resetLiveState = day.equals(yesterday) && hasPendingOperationalStateForDay(day, zone);
            closeOperationalDay(day, resetLiveState, "STARTUP");
            day = day.plusDays(1);
        }
    }

    // ─── PRIVATE: SNAPSHOT (MANUAL / SCHEDULED) ──────────────────────────────

    private Report saveDailySnapshotReport(LocalDate day, String reportType) {
        ZoneId zone = operationalTimeService.getBusinessZone();
        String periodKey = day.format(DateTimeFormatter.ISO_DATE);

        Date[] range = dayRange(day, zone);
        List<Client> activeClients = clientRepository.findByEntryTimestampBetween(range[0], range[1]);
        List<Map<String, Object>> clientSnapshot = snapshotClients(activeClients);

        List<Space> spaces = spaceRepository.findAll();
        List<Subsuelo> subsuelos = subsueloRepository.findAll();

        int totalSpaces    = spaces.size();
        int occupiedSpaces = (int) spaces.stream().filter(Space::isOccupied).count();
        int freeSpaces     = totalSpaces - occupiedSpaces;
        int occupancyRate  = totalSpaces > 0 ? Math.round((occupiedSpaces * 100f) / totalSpaces) : 0;

        Map<String, Long>         paymentAmounts = buildPaymentAmounts(clientSnapshot);
        long                      totalCobrado   = paymentAmounts.values().stream().mapToLong(Long::longValue).sum();
        Map<String, Integer>      timeStats      = buildTimeStats(clientSnapshot);
        List<Map<String, Object>> subsueloStats  = buildSubsueloStats(subsuelos, spaces);

        Report report = new Report();
        report.setTimestamp(OffsetDateTime.now(zone).toString());
        report.setPeriodType("DAILY");
        report.setPeriodKey(periodKey);
        report.setReportType(reportType);
        report.setDailyFinal(false);
        report.setTotalSpaces(totalSpaces);
        report.setOccupiedSpaces(occupiedSpaces);
        report.setFreeSpaces(freeSpaces);
        report.setOccupancyRate(occupancyRate);
        report.setTotalCobrado(totalCobrado);

        try {
            report.setFilteredClients(mapper.writeValueAsString(clientSnapshot));
            report.setPaymentAmounts(mapper.writeValueAsString(paymentAmounts));
            report.setTimeStats(mapper.writeValueAsString(timeStats));
            report.setSubsueloStats(mapper.writeValueAsString(subsueloStats));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializando snapshot diario", e);
        }

        Report saved = reportRepository.save(report);
        log.info("[SNAPSHOT] type={} day={} clients={} id={}", reportType, periodKey, clientSnapshot.size(), saved.getId());
        return saved;
    }

    // ─── PRIVATE: DAY_CLOSE from ServiceHistory ───────────────────────────────

    private Report generateDayCloseFromServiceHistory(LocalDate day, SpaceStats spaceStats) {
        ZoneId zone = operationalTimeService.getBusinessZone();
        String periodKey = day.format(DateTimeFormatter.ISO_DATE);

        List<ServiceHistory> histories = serviceHistoryService.getByDateRange(day, day);
        List<Map<String, Object>> clients = histories.stream()
                .map(this::serviceHistoryToMap)
                .collect(Collectors.toList());

        Map<String, Long>    paymentAmounts = buildPaymentAmountsFromHistory(histories);
        long                 totalCobrado   = paymentAmounts.values().stream().mapToLong(Long::longValue).sum();
        Map<String, Integer> timeStats      = buildTimeStatsFromHistory(histories);

        int totalSpaces = 0, occupiedSpaces = 0, freeSpaces = 0, occupancyRate = 0;
        List<Map<String, Object>> subsueloStats = List.of();
        if (spaceStats != null) {
            totalSpaces    = spaceStats.totalSpaces;
            occupiedSpaces = spaceStats.occupiedSpaces;
            freeSpaces     = spaceStats.freeSpaces;
            occupancyRate  = spaceStats.occupancyRate;
            subsueloStats  = spaceStats.subsueloStats;
        }

        Report report = reportRepository
                .findByPeriodTypeAndPeriodKeyAndReportType("DAILY", periodKey, "DAY_CLOSE")
                .orElse(new Report());
        report.setTimestamp(OffsetDateTime.now(zone).toString());
        report.setPeriodType("DAILY");
        report.setPeriodKey(periodKey);
        report.setReportType("DAY_CLOSE");
        report.setDailyFinal(true);
        report.setTotalSpaces(totalSpaces);
        report.setOccupiedSpaces(occupiedSpaces);
        report.setFreeSpaces(freeSpaces);
        report.setOccupancyRate(occupancyRate);
        report.setTotalCobrado(totalCobrado);

        try {
            report.setFilteredClients(mapper.writeValueAsString(clients));
            report.setPaymentAmounts(mapper.writeValueAsString(paymentAmounts));
            report.setTimeStats(mapper.writeValueAsString(timeStats));
            report.setSubsueloStats(mapper.writeValueAsString(subsueloStats));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializando DAY_CLOSE", e);
        }

        Report saved = reportRepository.save(report);
        log.info("[DAY-CLOSE] day={} clients={} totalCobrado={} id={}", periodKey, clients.size(), totalCobrado, saved.getId());
        return saved;
    }

    // ─── PRIVATE: SCHEDULED CLOSE HELPER ─────────────────────────────────────

    private void closeOperationalDay(LocalDate day, boolean resetLiveState, String source) {
        log.info("[DAILY-CLOSE] source={} targetDay={} resetLiveState={}", source, day, resetLiveState);
        try {
            if (resetLiveState) {
                finalizeDailyReportAndResetDay(day);
            } else {
                finalizeDailyReportForDay(day);
            }
            log.info("[DAILY-CLOSE] Cierre OK para {}", day);
        } catch (Exception e) {
            log.error("[DAILY-CLOSE] Error durante el cierre del dia {}", day, e);
        }
    }

    private boolean hasPendingOperationalStateForDay(LocalDate day, ZoneId zone) {
        long nextDayStartMs = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
        return spaceRepository.existsByOccupiedTrueAndStartTimeLessThan(nextDayStartMs);
    }

    private void generateMonthlyIfMonthEnded(LocalDate day) {
        boolean isLastDayOfMonth = !day.getMonth().equals(day.plusDays(1).getMonth());
        if (!isLastDayOfMonth) {
            return;
        }

        YearMonth ym = YearMonth.from(day);
        log.info("[MONTHLY-AUTO] Fin de mes detectado. Generando mensual para {}", ym);
        try {
            monthlyReportService.generateFromDailyReports(ym);
            log.info("[MONTHLY-AUTO] Mensual OK para {}", ym);
        } catch (Exception e) {
            log.error("[MONTHLY-AUTO] Error generando mensual para {}", ym, e);
        }
    }

    // ─── PRIVATE: SPACE STATS CAPTURE ────────────────────────────────────────

    private SpaceStats captureSpaceStats() {
        List<Space> spaces = spaceRepository.findAll();
        List<Subsuelo> subsuelos = subsueloRepository.findAll();
        SpaceStats s = new SpaceStats();
        s.totalSpaces    = spaces.size();
        s.occupiedSpaces = (int) spaces.stream().filter(Space::isOccupied).count();
        s.freeSpaces     = s.totalSpaces - s.occupiedSpaces;
        s.occupancyRate  = s.totalSpaces > 0 ? Math.round((s.occupiedSpaces * 100f) / s.totalSpaces) : 0;
        s.subsueloStats  = buildSubsueloStats(subsuelos, spaces);
        return s;
    }

    private static class SpaceStats {
        int totalSpaces, occupiedSpaces, freeSpaces, occupancyRate;
        List<Map<String, Object>> subsueloStats;
    }

    // ─── PRIVATE: BUILDERS ────────────────────────────────────────────────────

    private Map<String, Object> serviceHistoryToMap(ServiceHistory h) {
        Map<String, Object> row = new LinkedHashMap<>();
        String spaceDisplayName = resolveSpaceDisplayName(h.getSpaceKey());
        row.put("id",             h.getSourceClientId());
        row.put("code",           h.getCode());
        row.put("name",           h.getName());
        row.put("dni",            h.getDni());
        row.put("phoneIntl",      h.getPhoneIntl());
        row.put("phoneRaw",       h.getPhoneRaw());
        row.put("plate",          h.getPlate());
        row.put("notes",          h.getNotes());
        row.put("spaceKey",       h.getSpaceKey());
        row.put("vehicle",        h.getVehicle());
        row.put("category",       h.getCategory());
        row.put("price",          h.getPrice());
        row.put("paymentMethod",  h.getPaymentMethod());
        row.put("clover",         h.getClover());
        row.put("entryTimestamp", h.getEntryTimestamp());
        row.put("exitTimestamp",  h.getExitTimestamp());
        row.put("spaceDisplayName", spaceDisplayName);
        return row;
    }

    private Map<String, Long> buildPaymentAmountsFromHistory(List<ServiceHistory> histories) {
        Map<String, Long> amounts = new LinkedHashMap<>();
        for (String k : List.of("efectivo", "credito", "prepago", "qr", "debito", "scaneo", "S/Cargo", "otros")) {
            amounts.put(k, 0L);
        }
        for (ServiceHistory h : histories) {
            long amount = h.getPrice() != null ? h.getPrice().longValue() : 0L;
            String raw  = h.getPaymentMethod();
            String key  = raw == null ? "otros" : switch (raw.toLowerCase()) {
                case "efectivo" -> "efectivo";
                case "credito"  -> "credito";
                case "prepago"  -> "prepago";
                case "qr"       -> "qr";
                case "debito"   -> "debito";
                case "scaneo"   -> "scaneo";
                default -> "S/Cargo".equals(raw) ? "S/Cargo" : "otros";
            };
            amounts.put(key, amounts.get(key) + amount);
        }
        return amounts;
    }

    private Map<String, Integer> buildTimeStatsFromHistory(List<ServiceHistory> histories) {
        Map<String, Integer> stats = new LinkedHashMap<>();
        stats.put("under1h", 0);
        stats.put("between1h3h", 0);
        stats.put("over3h", 0);
        for (ServiceHistory h : histories) {
            Long entryTs = h.getEntryTimestamp();
            Long exitTs  = h.getExitTimestamp();
            if (entryTs == null || exitTs == null || exitTs <= entryTs) continue;
            double hours = (exitTs - entryTs) / 3600000.0;
            if (hours < 1)       stats.merge("under1h",     1, Integer::sum);
            else if (hours <= 3) stats.merge("between1h3h", 1, Integer::sum);
            else                 stats.merge("over3h",       1, Integer::sum);
        }
        return stats;
    }

    private List<Map<String, Object>> snapshotClients(List<Client> clients) {
        Map<String, String> spaceNames = buildSpaceDisplayNameLookup(
                clients.stream()
                        .map(Client::getSpaceKey)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet())
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (Client c : clients) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id",             c.getId());
            row.put("code",           c.getCode());
            row.put("name",           c.getName());
            row.put("dni",            c.getDni());
            row.put("phoneIntl",      c.getPhoneIntl());
            row.put("phoneRaw",       c.getPhoneRaw());
            row.put("plate",          c.getPlate());
            row.put("notes",          c.getNotes());
            row.put("spaceKey",       c.getSpaceKey());
            row.put("vehicle",        c.getVehicle());
            row.put("category",       c.getCategory());
            row.put("price",          c.getPrice());
            row.put("paymentMethod",  c.getPaymentMethod());
            row.put("clover",         c.getClover());
            row.put("entryTimestamp", c.getEntryTimestamp() != null ? c.getEntryTimestamp().getTime() : null);
            row.put("exitTimestamp",  c.getExitTimestamp());
            row.put("spaceDisplayName", spaceNames.getOrDefault(c.getSpaceKey(), c.getSpaceKey() != null ? c.getSpaceKey() : "-"));
            result.add(row);
        }
        return result;
    }

    private Map<String, String> buildSpaceDisplayNameLookup(Collection<String> spaceKeys) {
        if (spaceKeys == null || spaceKeys.isEmpty()) {
            return Map.of();
        }

        Map<String, String> lookup = new HashMap<>();
        for (Space space : spaceRepository.findAllById(spaceKeys)) {
            if (space == null || space.getKey() == null) {
                continue;
            }
            lookup.put(space.getKey(), resolveDisplayName(space));
        }
        return lookup;
    }

    private String resolveSpaceDisplayName(String spaceKey) {
        if (spaceKey == null || spaceKey.isBlank()) {
            return "-";
        }

        return spaceRepository.findById(spaceKey)
                .map(this::resolveDisplayName)
                .orElse(spaceKey);
    }

    private String resolveDisplayName(Space space) {
        if (space == null) {
            return "-";
        }
        return space.getDisplayName() != null && !space.getDisplayName().isBlank()
                ? space.getDisplayName()
                : (space.getKey() != null ? space.getKey() : "-");
    }

    private Map<String, Long> buildPaymentAmounts(List<Map<String, Object>> clients) {
        Map<String, Long> paymentAmounts = new LinkedHashMap<>();
        for (String key : List.of("efectivo", "credito", "prepago", "qr", "debito", "scaneo", "S/Cargo", "otros")) {
            paymentAmounts.put(key, 0L);
        }
        for (Map<String, Object> client : clients) {
            String method = String.valueOf(client.getOrDefault("paymentMethod", "otros"));
            long amount = 0L;
            try { amount = Long.parseLong(String.valueOf(client.getOrDefault("price", 0))); } catch (Exception ignored) {}
            String lower = method.toLowerCase();
            String key = switch (lower) {
                case "efectivo" -> "efectivo";
                case "credito"  -> "credito";
                case "prepago"  -> "prepago";
                case "qr"       -> "qr";
                case "debito"   -> "debito";
                case "scaneo"   -> "scaneo";
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
            Long entryTs = normalizeEpoch(client.get("entryTimestamp"));
            if (entryTs == null) continue;
            Long exitTs = normalizeEpoch(client.get("exitTimestamp"));
            long endTs  = (exitTs != null && exitTs > entryTs) ? exitTs : now;
            double hours = (endTs - entryTs) / 3600000.0;
            if (hours < 1)       stats.merge("under1h",     1, Integer::sum);
            else if (hours <= 3) stats.merge("between1h3h", 1, Integer::sum);
            else                 stats.merge("over3h",       1, Integer::sum);
        }
        return stats;
    }

    private List<Map<String, Object>> buildSubsueloStats(List<Subsuelo> subsuelos, List<Space> spaces) {
        Map<String, int[]> accumulator = new LinkedHashMap<>();
        for (Subsuelo subsuelo : subsuelos) accumulator.put(subsuelo.getId(), new int[]{0, 0});
        for (Space space : spaces) {
            String subsueloId = space.getSubsueloId();
            if (subsueloId == null) continue;
            int[] counts = accumulator.computeIfAbsent(subsueloId, ignored -> new int[]{0, 0});
            counts[0] += 1;
            if (space.isOccupied()) counts[1] += 1;
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Subsuelo subsuelo : subsuelos) {
            int[] counts = accumulator.getOrDefault(subsuelo.getId(), new int[]{0, 0});
            int total = counts[0], occupied = counts[1], free = total - occupied;
            int rate  = total > 0 ? Math.round((occupied * 100f) / total) : 0;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id",           subsuelo.getId());
            row.put("label",        subsuelo.getLabel());
            row.put("total",        total);
            row.put("occupied",     occupied);
            row.put("free",         free);
            row.put("occupancyRate", rate);
            result.add(row);
        }
        return result;
    }

    // ─── PRIVATE: UTILS ──────────────────────────────────────────────────────

    private LocalDate getLastClosedDay() {
        String periodKey = reportRepository.findMaxClosedDailyPeriodKey();
        if (periodKey == null || periodKey.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(periodKey);
        } catch (Exception ex) {
            log.warn("[DAILY-CLOSE] No se pudo parsear periodKey del ultimo cierre diario: {}", periodKey);
            return null;
        }
    }

    private LocalDate maxDay(LocalDate left, LocalDate right) {
        if (left == null) return right;
        if (right == null) return left;
        return left.isAfter(right) ? left : right;
    }

    private Date[] dayRange(LocalDate day, ZoneId zone) {
        return new Date[]{
                Date.from(day.atStartOfDay(zone).toInstant()),
                Date.from(day.plusDays(1).atStartOfDay(zone).toInstant())
        };
    }

    private Long normalizeEpoch(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        if (value instanceof Date d)   return d.getTime();
        String text = String.valueOf(value).trim();
        if (text.isEmpty() || "null".equalsIgnoreCase(text)) return null;
        try { return Long.parseLong(text); } catch (NumberFormatException ignored) {}
        try { return Date.from(OffsetDateTime.parse(text).toInstant()).getTime(); } catch (Exception ignored) {}
        try { return new Date(text).getTime(); } catch (Exception ignored) { return null; }
    }
}
