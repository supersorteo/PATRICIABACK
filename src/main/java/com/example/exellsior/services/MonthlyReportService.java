package com.example.exellsior.services;

import com.example.exellsior.entity.Report;
import com.example.exellsior.repository.ReportRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
public class MonthlyReportService {

    @Autowired private ReportRepository reportRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    public Report generateFromDailyReports(YearMonth ym) {
        String monthKey = ym.toString();

        List<Report> finalDailyReports = reportRepository
                .findByPeriodTypeAndPeriodKeyStartingWithAndDailyFinalTrue("DAILY", monthKey);
        List<Report> allDailyReports = reportRepository
                .findByPeriodTypeAndPeriodKeyStartingWith("DAILY", monthKey);

        Map<String, Report> latestAnyByDay = new HashMap<>();
        for (Report r : allDailyReports) {
            if (r == null || r.getPeriodKey() == null) continue;
            Report cur = latestAnyByDay.get(r.getPeriodKey());
            if (cur == null || parseTs(r.getTimestamp()).isAfter(parseTs(cur.getTimestamp()))) {
                latestAnyByDay.put(r.getPeriodKey(), r);
            }
        }

        Map<String, Report> finalByDay = new HashMap<>();
        for (Report r : finalDailyReports) {
            if (r == null || r.getPeriodKey() == null) continue;
            Report cur = finalByDay.get(r.getPeriodKey());
            if (cur == null || parseTs(r.getTimestamp()).isAfter(parseTs(cur.getTimestamp()))) {
                finalByDay.put(r.getPeriodKey(), r);
            }
        }

        Map<String, Report> selectedByDay = new HashMap<>(latestAnyByDay);
        finalByDay.forEach(selectedByDay::put);

        List<Report> dailyReports = new ArrayList<>(selectedByDay.values());
        dailyReports.sort(Comparator.comparing(Report::getTimestamp, Comparator.nullsLast(String::compareTo)));

        List<Map<String, Object>> monthlyClients = new ArrayList<>();
        Map<String, Long> paymentAmounts = new LinkedHashMap<>();
        for (String k : List.of("efectivo", "credito", "prepago", "qr", "debito", "scaneo", "S/Cargo", "otros")) {
            paymentAmounts.put(k, 0L);
        }

        Map<String, Integer> timeStats = new LinkedHashMap<>();
        timeStats.put("under1h", 0);
        timeStats.put("between1h3h", 0);
        timeStats.put("over3h", 0);

        Map<String, SubsueloAccumulator> subsueloAccumulators = new LinkedHashMap<>();

        long totalCobrado = 0L;
        int totalSpaces = 0;
        int occupiedSpacesSum = 0;
        int freeSpacesSum = 0;
        int occupancyRateSum = 0;
        int occupancySamples = 0;

        for (Report d : dailyReports) {
            try {
                List<Map<String, Object>> dayClients = mapper.readValue(
                        d.getFilteredClients() == null ? "[]" : d.getFilteredClients(),
                        new TypeReference<>() {});
                monthlyClients.addAll(dayClients);
            } catch (Exception e) {
                log.warn("[MONTHLY] Error parseando filteredClients del reporte ID {}", d.getId());
            }

            try {
                Map<String, Object> dayPay = mapper.readValue(
                        d.getPaymentAmounts() == null ? "{}" : d.getPaymentAmounts(),
                        new TypeReference<>() {});
                for (Map.Entry<String, Object> entry : dayPay.entrySet()) {
                    String k = entry.getKey();
                    long v = Long.parseLong(String.valueOf(entry.getValue()));
                    if (!paymentAmounts.containsKey(k)) k = "otros";
                    paymentAmounts.put(k, paymentAmounts.get(k) + v);
                }
            } catch (Exception ignored) {}

            mergeTimeStats(timeStats, d.getTimeStats());
            mergeSubsueloStats(subsueloAccumulators, d.getSubsueloStats());

            totalCobrado += d.getTotalCobrado() == null ? 0L : d.getTotalCobrado();
            totalSpaces = Math.max(totalSpaces, d.getTotalSpaces());
            occupiedSpacesSum += d.getOccupiedSpaces();
            freeSpacesSum += d.getFreeSpaces();
            occupancyRateSum += d.getOccupancyRate();
            occupancySamples += 1;
        }

        int occupiedSpaces = occupancySamples > 0 ? Math.round(occupiedSpacesSum / (float) occupancySamples) : 0;
        int freeSpaces = occupancySamples > 0 ? Math.round(freeSpacesSum / (float) occupancySamples) : 0;
        int occupancyRate = occupancySamples > 0 ? Math.round(occupancyRateSum / (float) occupancySamples) : 0;
        List<Map<String, Object>> subsueloStats = buildMonthlySubsueloStats(subsueloAccumulators);

        Report monthly = reportRepository
                .findByPeriodTypeAndPeriodKey("MONTHLY", monthKey)
                .orElse(new Report());

        monthly.setTimestamp(OffsetDateTime.now(ZoneId.of("America/Argentina/Buenos_Aires")).toString());
        monthly.setPeriodType("MONTHLY");
        monthly.setPeriodKey(monthKey);
        monthly.setDailyFinal(false);
        monthly.setTotalSpaces(totalSpaces);
        monthly.setOccupiedSpaces(occupiedSpaces);
        monthly.setFreeSpaces(freeSpaces);
        monthly.setOccupancyRate(occupancyRate);
        monthly.setTotalCobrado(totalCobrado);

        try {
            monthly.setSubsueloStats(mapper.writeValueAsString(subsueloStats));
            monthly.setTimeStats(mapper.writeValueAsString(timeStats));
            monthly.setFilteredClients(mapper.writeValueAsString(monthlyClients));
            monthly.setPaymentAmounts(mapper.writeValueAsString(paymentAmounts));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializando reporte mensual", e);
        }

        return reportRepository.save(monthly);
    }

    @Scheduled(cron = "30 59 23 * * *", zone = "America/Argentina/Buenos_Aires")
    public void autoMonthlyEndOfMonth() {
        ZoneId zone = ZoneId.of("America/Argentina/Buenos_Aires");
        LocalDate now = LocalDate.now(zone);
        boolean isEndOfMonth = now.getMonthValue() != now.plusDays(1).getMonthValue();
        log.info("[MONTHLY-CHECK] now={} isEndOfMonth={}", now, isEndOfMonth);
        if (!isEndOfMonth) return;
        YearMonth ym = YearMonth.from(now);
        log.info("[MONTHLY-GENERATE] Generando mensual para {}", ym);
        generateFromDailyReports(ym);
    }

    private OffsetDateTime parseTs(String ts) {
        if (ts == null || ts.isBlank()) return OffsetDateTime.MIN;
        try { return OffsetDateTime.parse(ts); } catch (Exception e) { return OffsetDateTime.MIN; }
    }

    private void mergeTimeStats(Map<String, Integer> target, String rawTimeStats) {
        try {
            Map<String, Object> parsed = mapper.readValue(
                    rawTimeStats == null ? "{}" : rawTimeStats,
                    new TypeReference<>() {});
            for (String key : List.of("under1h", "between1h3h", "over3h")) {
                int value = Integer.parseInt(String.valueOf(parsed.getOrDefault(key, 0)));
                target.put(key, target.getOrDefault(key, 0) + value);
            }
        } catch (Exception ignored) {}
    }

    private void mergeSubsueloStats(Map<String, SubsueloAccumulator> target, String rawSubsueloStats) {
        try {
            List<Map<String, Object>> parsed = mapper.readValue(
                    rawSubsueloStats == null ? "[]" : rawSubsueloStats,
                    new TypeReference<>() {});

            for (Map<String, Object> row : parsed) {
                String id = String.valueOf(row.getOrDefault("id", ""));
                if (id.isBlank()) continue;

                SubsueloAccumulator acc = target.computeIfAbsent(id, ignored -> new SubsueloAccumulator());
                acc.id = id;
                acc.label = String.valueOf(row.getOrDefault("label", id));
                acc.total = Math.max(acc.total, asInt(row.get("total")));
                acc.occupiedSum += asInt(row.get("occupied"));
                acc.freeSum += asInt(row.get("free"));
                acc.occupancyRateSum += asInt(row.get("occupancyRate"));
                acc.samples += 1;
            }
        } catch (Exception ignored) {}
    }

    private List<Map<String, Object>> buildMonthlySubsueloStats(Map<String, SubsueloAccumulator> accumulators) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (SubsueloAccumulator acc : accumulators.values()) {
            int samples = Math.max(acc.samples, 1);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", acc.id);
            row.put("label", acc.label);
            row.put("total", acc.total);
            row.put("occupied", Math.round(acc.occupiedSum / (float) samples));
            row.put("free", Math.round(acc.freeSum / (float) samples));
            row.put("occupancyRate", Math.round(acc.occupancyRateSum / (float) samples));
            result.add(row);
        }
        result.sort(Comparator.comparing(row -> String.valueOf(row.getOrDefault("label", ""))));
        return result;
    }

    private int asInt(Object value) {
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static class SubsueloAccumulator {
        private String id;
        private String label;
        private int total;
        private int occupiedSum;
        private int freeSum;
        private int occupancyRateSum;
        private int samples;
    }
}
