package com.example.exellsior.services;

import com.example.exellsior.entity.Client;
import com.example.exellsior.entity.Report;
import com.example.exellsior.repository.ClientRepository;
import com.example.exellsior.repository.ReportRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.example.exellsior.entity.Space;
import org.springframework.context.event.EventListener;

import com.example.exellsior.repository.SpaceRepository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.*;

@Service
public class ReportService {
    @Autowired
    private ReportRepository reportRepository;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private ClientService clientService;


    private final ObjectMapper mapper = new ObjectMapper();

    public Report saveReport0(Report report) {
        return reportRepository.save(report);
    }

    public Report saveReport1(Report report) {
        // si frontend no envía periodType/key, completar como DAILY
        if (report.getPeriodType() == null || report.getPeriodType().isBlank()) {
            report.setPeriodType("DAILY");
        }
        if (report.getPeriodKey() == null || report.getPeriodKey().isBlank()) {
            String dayKey = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
            report.setPeriodKey(dayKey);
        }
        return reportRepository.save(report);
    }

    public Report saveReport(Report report) {
        if (report.getPeriodType() == null || report.getPeriodType().isBlank()) {
            report.setPeriodType("DAILY");
        }

        if (report.getPeriodKey() == null || report.getPeriodKey().isBlank()) {
            String dayKey = LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires"))
                    .format(DateTimeFormatter.ISO_DATE);
            report.setPeriodKey(dayKey);
        }

        if (report.getTimestamp() == null || report.getTimestamp().isBlank()) {
            report.setTimestamp(OffsetDateTime.now(ZoneId.of("America/Argentina/Buenos_Aires")).toString());
        }

        // DAILY manual/auto por defecto NO final
        if ("DAILY".equalsIgnoreCase(report.getPeriodType()) && report.getDailyFinal() == null) {
            report.setDailyFinal(false);
        }

        return reportRepository.save(report);
    }


    public List<Report> getAllReports() {
        return reportRepository.findAll();
    }

    public Optional<Report> getReportById(Long id) {
        return reportRepository.findById(id);
    }

    public void deleteReport(Long id) {
        reportRepository.deleteById(id);
    }






   /* public Report generateMonthlyFromDailyReports(YearMonth ym) {
        String monthKey = ym.toString(); // yyyy-MM

        List<Report> dailyReports = reportRepository
                .findByPeriodTypeAndPeriodKeyStartingWith("DAILY", monthKey);

        List<Map<String, Object>> monthlyClients = new ArrayList<>();
        Map<String, Long> paymentAmounts = new LinkedHashMap<>();
        paymentAmounts.put("efectivo", 0L);
        paymentAmounts.put("credito", 0L);
        paymentAmounts.put("prepago", 0L);
        paymentAmounts.put("qr", 0L);
        paymentAmounts.put("debito", 0L);
        paymentAmounts.put("scaneo", 0L);
        paymentAmounts.put("S/Cargo", 0L);
        paymentAmounts.put("otros", 0L);

        long totalCobrado = 0L;
        int totalSpaces = 0;
        int occupiedSpaces = 0;
        int freeSpaces = 0;
        int occupancyRate = 0;

        for (Report d : dailyReports) {
            try {
                List<Map<String, Object>> dayClients = mapper.readValue(
                        d.getFilteredClients(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
                monthlyClients.addAll(dayClients);
            } catch (Exception e) {
                System.out.println("Error parseando filteredClients del reporte diario ID " + d.getId());
            }

            try {
                Map<String, Object> dayPay = mapper.readValue(
                        d.getPaymentAmounts() == null ? "{}" : d.getPaymentAmounts(),
                        new TypeReference<Map<String, Object>>() {}
                );
                for (Map.Entry<String, Object> entry : dayPay.entrySet()) {
                    String k = entry.getKey();
                    long v = Long.parseLong(String.valueOf(entry.getValue()));
                    if (!paymentAmounts.containsKey(k)) k = "otros";
                    paymentAmounts.put(k, paymentAmounts.get(k) + v);
                }
            } catch (Exception ignored) {}

            totalCobrado += (d.getTotalCobrado() == null ? 0L : d.getTotalCobrado());

            // snapshot del último diario del mes
            totalSpaces = d.getTotalSpaces();
            occupiedSpaces = d.getOccupiedSpaces();
            freeSpaces = d.getFreeSpaces();
            occupancyRate = d.getOccupancyRate();
        }

        Report monthly = reportRepository
                .findByPeriodTypeAndPeriodKey("MONTHLY", monthKey)
                .orElse(new Report());

        monthly.setTimestamp(OffsetDateTime.now().toString());
        monthly.setPeriodType("MONTHLY");
        monthly.setPeriodKey(monthKey);
        monthly.setTotalSpaces(totalSpaces);
        monthly.setOccupiedSpaces(occupiedSpaces);
        monthly.setFreeSpaces(freeSpaces);
        monthly.setOccupancyRate(occupancyRate);

        try {
            monthly.setFilteredClients(mapper.writeValueAsString(monthlyClients));
            monthly.setPaymentAmounts(mapper.writeValueAsString(paymentAmounts));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializando reporte mensual", e);
        }

        monthly.setSubsueloStats("[]");
        monthly.setTimeStats("{}");
        monthly.setTotalCobrado(totalCobrado);

        return reportRepository.save(monthly);
    }*/

    public Report generateMonthlyFromDailyReports(YearMonth ym) {
        String monthKey = ym.toString(); // yyyy-MM

        // 1) Preferir reportes DAILY finales
        List<Report> finalDailyReports = reportRepository
                .findByPeriodTypeAndPeriodKeyStartingWithAndDailyFinalTrue("DAILY", monthKey);

        // 2) Todos los DAILY (para fallback por día si no hay final)
        List<Report> allDailyReports = reportRepository
                .findByPeriodTypeAndPeriodKeyStartingWith("DAILY", monthKey);

        // 3) Quedarse con 1 reporte por día:
        //    - si existe final para el día -> usar ese
        //    - si no existe final -> usar el último DAILY por timestamp
        Map<String, Report> latestAnyByDay = new HashMap<>();
        for (Report r : allDailyReports) {
            if (r == null || r.getPeriodKey() == null) continue;
            Report current = latestAnyByDay.get(r.getPeriodKey());
            if (current == null || parseTs(r.getTimestamp()).isAfter(parseTs(current.getTimestamp()))) {
                latestAnyByDay.put(r.getPeriodKey(), r);
            }
        }

        Map<String, Report> finalByDay = new HashMap<>();
        for (Report r : finalDailyReports) {
            if (r == null || r.getPeriodKey() == null) continue;
            Report current = finalByDay.get(r.getPeriodKey());
            if (current == null || parseTs(r.getTimestamp()).isAfter(parseTs(current.getTimestamp()))) {
                finalByDay.put(r.getPeriodKey(), r);
            }
        }

        // Selección final por día
        Map<String, Report> selectedByDay = new HashMap<>(latestAnyByDay);
        finalByDay.forEach(selectedByDay::put); // final pisa fallback

        List<Report> dailyReports = new ArrayList<>(selectedByDay.values());
        dailyReports.sort(Comparator.comparing(Report::getTimestamp, Comparator.nullsLast(String::compareTo)));

        List<Map<String, Object>> monthlyClients = new ArrayList<>();
        Map<String, Long> paymentAmounts = new LinkedHashMap<>();
        paymentAmounts.put("efectivo", 0L);
        paymentAmounts.put("credito", 0L);
        paymentAmounts.put("prepago", 0L);
        paymentAmounts.put("qr", 0L);
        paymentAmounts.put("debito", 0L);
        paymentAmounts.put("scaneo", 0L);
        paymentAmounts.put("S/Cargo", 0L);
        paymentAmounts.put("otros", 0L);

        long totalCobrado = 0L;
        int totalSpaces = 0;
        int occupiedSpaces = 0;
        int freeSpaces = 0;
        int occupancyRate = 0;

        for (Report d : dailyReports) {
            try {
                List<Map<String, Object>> dayClients = mapper.readValue(
                        d.getFilteredClients() == null ? "[]" : d.getFilteredClients(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
                monthlyClients.addAll(dayClients);
            } catch (Exception e) {
                System.out.println("Error parseando filteredClients del reporte diario ID " + d.getId());
            }

            try {
                Map<String, Object> dayPay = mapper.readValue(
                        d.getPaymentAmounts() == null ? "{}" : d.getPaymentAmounts(),
                        new TypeReference<Map<String, Object>>() {}
                );
                for (Map.Entry<String, Object> entry : dayPay.entrySet()) {
                    String k = entry.getKey();
                    long v = Long.parseLong(String.valueOf(entry.getValue()));
                    if (!paymentAmounts.containsKey(k)) k = "otros";
                    paymentAmounts.put(k, paymentAmounts.get(k) + v);
                }
            } catch (Exception ignored) {}

            totalCobrado += (d.getTotalCobrado() == null ? 0L : d.getTotalCobrado());

            // snapshot del último diario usado del mes
            totalSpaces = d.getTotalSpaces();
            occupiedSpaces = d.getOccupiedSpaces();
            freeSpaces = d.getFreeSpaces();
            occupancyRate = d.getOccupancyRate();
        }

        Report monthly = reportRepository
                .findByPeriodTypeAndPeriodKey("MONTHLY", monthKey)
                .orElse(new Report());

        monthly.setTimestamp(OffsetDateTime.now(ZoneId.of("America/Argentina/Buenos_Aires")).toString());
        monthly.setPeriodType("MONTHLY");
        monthly.setPeriodKey(monthKey);
        monthly.setDailyFinal(false); // no aplica a monthly
        monthly.setTotalSpaces(totalSpaces);
        monthly.setOccupiedSpaces(occupiedSpaces);
        monthly.setFreeSpaces(freeSpaces);
        monthly.setOccupancyRate(occupancyRate);

        try {
            monthly.setFilteredClients(mapper.writeValueAsString(monthlyClients));
            monthly.setPaymentAmounts(mapper.writeValueAsString(paymentAmounts));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializando reporte mensual", e);
        }

        monthly.setSubsueloStats("[]");
        monthly.setTimeStats("{}");
        monthly.setTotalCobrado(totalCobrado);

        return reportRepository.save(monthly);
    }


    public Report createOrGenerate0(Report report) {
        String periodType = (report.getPeriodType() == null || report.getPeriodType().isBlank())
                ? "DAILY"
                : report.getPeriodType().trim().toUpperCase();

        if ("MONTHLY".equals(periodType)) {
            YearMonth ym = resolveYearMonth(report);
            return generateMonthlyFromDailyReports(ym);
        }

        // DAILY
        report.setPeriodType("DAILY");
        if (report.getPeriodKey() == null || report.getPeriodKey().isBlank()) {
            report.setPeriodKey(LocalDate.now().toString()); // yyyy-MM-dd
        }
        if (report.getTimestamp() == null || report.getTimestamp().isBlank()) {
            report.setTimestamp(OffsetDateTime.now().toString());
        }
        return reportRepository.save(report);
    }


    public Report createOrGenerate(Report report) {
        String periodType = (report.getPeriodType() == null || report.getPeriodType().isBlank())
                ? "DAILY"
                : report.getPeriodType().trim().toUpperCase();

        if ("MONTHLY".equals(periodType)) {
            YearMonth ym = resolveYearMonth(report);
            return generateMonthlyFromDailyReports(ym);
        }

        // DAILY
        report.setPeriodType("DAILY");

        if (report.getPeriodKey() == null || report.getPeriodKey().isBlank()) {
            report.setPeriodKey(LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires")).toString());
        }

        if (report.getTimestamp() == null || report.getTimestamp().isBlank()) {
            report.setTimestamp(OffsetDateTime.now(ZoneId.of("America/Argentina/Buenos_Aires")).toString());
        }

        if (report.getDailyFinal() == null) {
            report.setDailyFinal(false); // manual/auto = preview/intermedio
        }

        return reportRepository.save(report);
    }


    private YearMonth resolveYearMonth(Report report) {
        if (report.getPeriodKey() != null && !report.getPeriodKey().isBlank()) {
            return YearMonth.parse(report.getPeriodKey()); // yyyy-MM
        }
        if (report.getTimestamp() != null && !report.getTimestamp().isBlank()) {
            return YearMonth.from(OffsetDateTime.parse(report.getTimestamp()));
        }
        return YearMonth.now();
    }



    private String toDailyPeriodKey(LocalDate day) {
        return day.format(DateTimeFormatter.ISO_DATE); // yyyy-MM-dd
    }

    private Date[] getArgentinaDayRange(LocalDate day) {
        ZoneId zone = ZoneId.of("America/Argentina/Buenos_Aires");
        Date from = Date.from(day.atStartOfDay(zone).toInstant());
        Date to = Date.from(day.plusDays(1).atStartOfDay(zone).toInstant());
        return new Date[]{from, to};
    }

    private List<Map<String, Object>> parseClientsJsonSafe(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return mapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> mergeAndDedupReportClients(
            List<Map<String, Object>> existingClients,
            List<Map<String, Object>> currentClients
    ) {
        List<Map<String, Object>> merged = new ArrayList<>();
        if (existingClients != null) merged.addAll(existingClients);
        if (currentClients != null) merged.addAll(currentClients);

        Map<String, Map<String, Object>> dedup = new LinkedHashMap<>();

        for (Map<String, Object> c : merged) {
            String key = String.valueOf(c.getOrDefault("id", "x")) + "|" +
                    String.valueOf(c.getOrDefault("code", "x")) + "|" +
                    String.valueOf(c.getOrDefault("entryTimestamp", "x")) + "|" +
                    String.valueOf(c.getOrDefault("exitTimestamp", "x"));
            dedup.put(key, c); // último gana
        }

        return new ArrayList<>(dedup.values());
    }

    private Map<String, Long> buildPaymentAmountsFromClientMaps(List<Map<String, Object>> clients) {
        Map<String, Long> paymentAmounts = new LinkedHashMap<>();
        paymentAmounts.put("efectivo", 0L);
        paymentAmounts.put("credito", 0L);
        paymentAmounts.put("prepago", 0L);
        paymentAmounts.put("qr", 0L);
        paymentAmounts.put("debito", 0L);
        paymentAmounts.put("scaneo", 0L);
        paymentAmounts.put("S/Cargo", 0L);
        paymentAmounts.put("otros", 0L);

        for (Map<String, Object> c : clients) {
            String methodRaw = String.valueOf(c.getOrDefault("paymentMethod", "otros"));
            long amount = 0L;
            try {
                amount = Long.parseLong(String.valueOf(c.getOrDefault("price", 0)));
            } catch (Exception ignored) {}

            String lower = methodRaw.toLowerCase();
            String key;
            if ("efectivo".equals(lower)) key = "efectivo";
            else if ("credito".equals(lower)) key = "credito";
            else if ("prepago".equals(lower)) key = "prepago";
            else if ("qr".equals(lower)) key = "qr";
            else if ("debito".equals(lower)) key = "debito";
            else if ("scaneo".equals(lower)) key = "scaneo";
            else if ("S/Cargo".equals(methodRaw)) key = "S/Cargo";
            else key = "otros";

            paymentAmounts.put(key, paymentAmounts.get(key) + amount);
        }

        return paymentAmounts;
    }

    private Map<String, Integer> buildTimeStatsFromClientMaps(List<Map<String, Object>> clients) {
        Map<String, Integer> stats = new LinkedHashMap<>();
        stats.put("under1h", 0);
        stats.put("between1h3h", 0);
        stats.put("over3h", 0);

        long now = System.currentTimeMillis();

        for (Map<String, Object> c : clients) {
            Object entryObj = c.get("entryTimestamp");
            if (entryObj == null) continue;

            Long entryTs = null;
            try {
                entryTs = Long.parseLong(String.valueOf(entryObj));
            } catch (Exception ignored) {}

            if (entryTs == null) continue;

            double hours = (now - entryTs) / 3600000.0;
            if (hours < 1) stats.put("under1h", stats.get("under1h") + 1);
            else if (hours <= 3) stats.put("between1h3h", stats.get("between1h3h") + 1);
            else stats.put("over3h", stats.get("over3h") + 1);
        }

        return stats;
    }

    public Report finalizeDailyReportForDay(LocalDate day) {
        ZoneId zone = ZoneId.of("America/Argentina/Buenos_Aires");
        String periodKey = toDailyPeriodKey(day);

        // 1) Clientes actuales del día (desde DB)
        Date[] range = getArgentinaDayRange(day);
        List<Client> todayClients = clientRepository.findByEntryTimestampBetween(range[0], range[1]);

        List<Map<String, Object>> currentClients = new ArrayList<>();
        for (Client c : todayClients) {
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
            currentClients.add(row);
        }

        // 2) Reportes DAILY existentes del mismo día (manuales/finales/legacy)
        List<Report> sameDayDailyReports = reportRepository.findAllByPeriodTypeAndPeriodKey("DAILY", periodKey);

        List<Map<String, Object>> existingClients = new ArrayList<>();
        for (Report r : sameDayDailyReports) {
            existingClients.addAll(parseClientsJsonSafe(r.getFilteredClients()));
        }

        // 3) Fusionar + deduplicar
        List<Map<String, Object>> mergedClients = mergeAndDedupReportClients(existingClients, currentClients);

        // 4) Si no hay nada, no crear reporte
        if (sameDayDailyReports.isEmpty() && mergedClients.isEmpty()) {
            System.out.println("[DAILY-FINALIZE] No hay datos para el día " + periodKey);
            return null;
        }

        // 5) Snapshot de espacios al momento del cierre
        List<Space> spaces = spaceRepository.findAll();
        int totalSpaces = spaces.size();
        int occupiedSpaces = 0;
        for (Space s : spaces) {
            if (s.isOccupied()) occupiedSpaces++;
        }
        int freeSpaces = totalSpaces - occupiedSpaces;
        int occupancyRate = totalSpaces > 0 ? Math.round((occupiedSpaces * 100f) / totalSpaces) : 0;

        // 6) Recalcular montos/tiempos desde lista fusionada
        Map<String, Long> paymentAmounts = buildPaymentAmountsFromClientMaps(mergedClients);
        long totalCobrado = paymentAmounts.values().stream().mapToLong(Long::longValue).sum();
        Map<String, Integer> timeStats = buildTimeStatsFromClientMaps(mergedClients);

        // 7) Crear reporte final consolidado del día
        Report finalReport = new Report();
        finalReport.setTimestamp(OffsetDateTime.now(zone).toString());
        finalReport.setPeriodType("DAILY");
        finalReport.setPeriodKey(periodKey);
        finalReport.setDailyFinal(true);

        finalReport.setTotalSpaces(totalSpaces);
        finalReport.setOccupiedSpaces(occupiedSpaces);
        finalReport.setFreeSpaces(freeSpaces);
        finalReport.setOccupancyRate(occupancyRate);

        // Puedes mejorar subsueloStats si luego agregas SubsueloRepository aquí
        finalReport.setSubsueloStats("[]");

        try {
            finalReport.setFilteredClients(mapper.writeValueAsString(mergedClients));
            finalReport.setPaymentAmounts(mapper.writeValueAsString(paymentAmounts));
            finalReport.setTimeStats(mapper.writeValueAsString(timeStats));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializando reporte diario final", e);
        }

        finalReport.setTotalCobrado(totalCobrado);

        // 8) Reemplazar todos los DAILY del día por uno final consolidado
        if (!sameDayDailyReports.isEmpty()) {
            reportRepository.deleteAll(sameDayDailyReports);
        }

        Report saved = reportRepository.save(finalReport);

        System.out.println("[DAILY-FINALIZE] Reporte final diario guardado. day=" + periodKey +
                " reportId=" + saved.getId() +
                " mergedClients=" + mergedClients.size());

        return saved;
    }

    public void finalizeDailyReportAndResetDay(LocalDate day) {
        Report finalReport = finalizeDailyReportForDay(day);
        System.out.println("[DAILY-CLOSE] Finalize result day=" + day +
                " reportId=" + (finalReport != null ? finalReport.getId() : "null"));

        clientService.resetAllData();
    }


    // @Scheduled(cron = "0 59 23 * * *", zone = "America/Argentina/Buenos_Aires")
    @Scheduled(cron = "30 59 23 * * *", zone = "America/Argentina/Buenos_Aires")
    public void autoMonthlyEndOfMonth() {
        ZoneId zone = ZoneId.of("America/Argentina/Buenos_Aires");
        LocalDate now = LocalDate.now(zone);
        LocalDate tomorrow = now.plusDays(1);

        boolean isEndOfMonth = now.getMonthValue() != tomorrow.getMonthValue();

        System.out.println("[MONTHLY-CHECK] now=" + now +
                " tomorrow=" + tomorrow +
                " isEndOfMonth=" + isEndOfMonth);

        if (!isEndOfMonth) return;

        YearMonth ym = YearMonth.from(now);
        System.out.println("[MONTHLY-GENERATE] Generando mensual para " + ym);
        generateMonthlyFromDailyReports(ym);
    }


   /* @Scheduled(cron = "0 59 23 * * *", zone = "America/Argentina/Buenos_Aires")
    public void autoDailyCloseEndOfDay() {
        ZoneId zone = ZoneId.of("America/Argentina/Buenos_Aires");
        LocalDate today = LocalDate.now(zone);

        System.out.println("[DAILY-AUTO-CLOSE] Ejecutando cierre automático para " + today);

        try {
            finalizeDailyReportAndResetDay(today);
            System.out.println("[DAILY-AUTO-CLOSE] Cierre automático completado para " + today);
        } catch (Exception e) {
            System.err.println("[DAILY-AUTO-CLOSE] Error en cierre automático: " + e.getMessage());
            e.printStackTrace();
        }
    }*/


    @Scheduled(cron = "0 59 23 * * *", zone = "America/Argentina/Buenos_Aires")
    public void autoDailyCloseEndOfDay() {
        runDailyCloseIfNeeded("SCHEDULED");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void catchUpDailyCloseOnStartup0() {
        runDailyCloseIfNeeded("STARTUP");
    }


    @EventListener(ApplicationReadyEvent.class)
    public void catchUpDailyCloseOnStartup() {
        ZoneId zone = ZoneId.of("America/Argentina/Buenos_Aires");
        LocalDate today = LocalDate.now(zone);
        LocalDate yesterday = today.minusDays(1);

        LocalDate lastClosedDay = getLastClosedDayFromClients(zone);

        // Si nunca hubo cierre registrado, intentamos al menos el día anterior
        LocalDate startDay = (lastClosedDay == null) ? yesterday : lastClosedDay.plusDays(1);

        if (startDay.isAfter(yesterday)) {
            System.out.println("[DAILY-CATCHUP] No hay días pendientes por cerrar.");
            return;
        }

        System.out.println("[DAILY-CATCHUP] Ejecutando catch-up multi-día. startDay=" + startDay + " yesterday=" + yesterday);

        LocalDate day = startDay;
        while (!day.isAfter(yesterday)) {
            System.out.println("[DAILY-CATCHUP] Cerrando día pendiente: " + day);

            try {
                try {
                    finalizeDailyReportForDay(day);
                    System.out.println("[DAILY-CATCHUP] Reporte final OK para " + day);
                } catch (Exception reportErr) {
                    System.err.println("[DAILY-CATCHUP] Falló reporte final para " + day + ", se continúa reset: " + reportErr.getMessage());
                    reportErr.printStackTrace();
                }

                clientService.resetAllData();
                System.out.println("[DAILY-CATCHUP] Reset OK para " + day);
            } catch (Exception resetErr) {
                System.err.println("[DAILY-CATCHUP] Error en reset para " + day + ": " + resetErr.getMessage());
                resetErr.printStackTrace();
                // si un día falla, cortar para evitar más daño en cadena
                break;
            }

            day = day.plusDays(1);
        }
    }



    private OffsetDateTime parseTs(String ts) {
        if (ts == null || ts.isBlank()) return OffsetDateTime.MIN;
        try {
            return OffsetDateTime.parse(ts);
        } catch (Exception e) {
            return OffsetDateTime.MIN;
        }
    }


    private void runDailyCloseIfNeeded(String source) {
        ZoneId zone = ZoneId.of("America/Argentina/Buenos_Aires");
        LocalDate today = LocalDate.now(zone);
        long todayStartMs = today.atStartOfDay(zone).toInstant().toEpochMilli();

        List<com.example.exellsior.entity.Space> spaces = spaceRepository.findAll();

        boolean staleOccupied = spaces.stream().anyMatch(s ->
                s.isOccupied() &&
                        s.getStartTime() != null &&
                        s.getStartTime() < todayStartMs
        );

        // Si arranca app y no hay nada pendiente, no hacer nada
        if ("STARTUP".equals(source) && !staleOccupied) {
            System.out.println("[DAILY-CATCHUP] No hay espacios pendientes de cierre.");
            return;
        }

        // En startup, si quedó pendiente, cerrar el día anterior.
        // En scheduler normal (23:59), también cerramos el día actual.
        LocalDate targetDay = "STARTUP".equals(source) ? today.minusDays(1) : today;

        System.out.println("[DAILY-CLOSE] source=" + source +
                " today=" + today +
                " targetDay=" + targetDay +
                " staleOccupied=" + staleOccupied);

        try {
            try {
                // consolidar reporte final diario
                finalizeDailyReportForDay(targetDay);
                System.out.println("[DAILY-CLOSE] Reporte final diario OK para " + targetDay);
            } catch (Exception reportErr) {
                // no bloquea reset
                System.err.println("[DAILY-CLOSE] Falló generación de reporte final, se continúa con reset: " + reportErr.getMessage());
                reportErr.printStackTrace();
            }

            // reset operativo de espacios/clientes
            clientService.resetAllData();
            System.out.println("[DAILY-CLOSE] Reset OK para " + targetDay);
        } catch (Exception resetErr) {
            System.err.println("[DAILY-CLOSE] Error durante reset: " + resetErr.getMessage());
            resetErr.printStackTrace();
        }
    }


    private LocalDate getLastClosedDayFromClients(ZoneId zone) {
        List<Client> clients = clientRepository.findAll();

        long maxLastDayClosedMs = 0L;
        for (Client c : clients) {
            if (c.getLastDayClosed() != null && c.getLastDayClosed() > maxLastDayClosedMs) {
                maxLastDayClosedMs = c.getLastDayClosed();
            }
        }

        if (maxLastDayClosedMs <= 0L) return null;

        return new Date(maxLastDayClosedMs).toInstant().atZone(zone).toLocalDate();
    }

}
