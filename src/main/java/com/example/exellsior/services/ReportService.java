package com.example.exellsior.services;

import com.example.exellsior.dto.ClientDTO;
import com.example.exellsior.dto.PagedResponse;
import com.example.exellsior.entity.Report;
import com.example.exellsior.entity.ServiceHistory;
import com.example.exellsior.repository.ReportRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReportService {

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired private ReportRepository reportRepository;
    @Autowired private DailyReportService dailyReportService;
    @Autowired private MonthlyReportService monthlyReportService;
    @Autowired private ServiceHistoryService serviceHistoryService;
    @Autowired private OperationalTimeService operationalTimeService;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migrateReportTypes() {
        List<Report> toMigrate = reportRepository.findByReportTypeIsNull();
        if (toMigrate.isEmpty()) return;
        toMigrate.forEach(r -> {
            if ("MONTHLY".equalsIgnoreCase(r.getPeriodType())) {
                r.setReportType("MONTHLY");
            } else if (Boolean.TRUE.equals(r.getDailyFinal())) {
                r.setReportType("DAY_CLOSE");
            } else {
                r.setReportType("MANUAL");
            }
        });
        reportRepository.saveAll(toMigrate);
        log.info("[MIGRATION] reportType migrado en {} registros", toMigrate.size());
    }

    public Report saveReport(Report report) {
        if (report.getPeriodType() == null || report.getPeriodType().isBlank()) {
            report.setPeriodType("DAILY");
        }
        if (report.getPeriodKey() == null || report.getPeriodKey().isBlank()) {
            report.setPeriodKey(LocalDate.now(operationalTimeService.getBusinessZone())
                    .format(DateTimeFormatter.ISO_DATE));
        }
        if (report.getTimestamp() == null || report.getTimestamp().isBlank()) {
            report.setTimestamp(OffsetDateTime.now(operationalTimeService.getBusinessZone()).toString());
        }
        if ("DAILY".equalsIgnoreCase(report.getPeriodType()) && report.getDailyFinal() == null) {
            report.setDailyFinal(false);
        }
        return reportRepository.save(report);
    }

    public Report createOrGenerate(Report report) {
        String periodType = (report.getPeriodType() == null || report.getPeriodType().isBlank())
                ? "DAILY"
                : report.getPeriodType().trim().toUpperCase();

        if ("MONTHLY".equals(periodType)) {
            return monthlyReportService.generateFromDailyReports(resolveYearMonth(report));
        }

        report.setPeriodType("DAILY");
        if (report.getPeriodKey() == null || report.getPeriodKey().isBlank()) {
            report.setPeriodKey(LocalDate.now(operationalTimeService.getBusinessZone()).toString());
        }
        if (report.getTimestamp() == null || report.getTimestamp().isBlank()) {
            report.setTimestamp(OffsetDateTime.now(operationalTimeService.getBusinessZone()).toString());
        }
        if (report.getDailyFinal() == null) {
            report.setDailyFinal(false);
        }
        if (report.getReportType() == null || report.getReportType().isBlank()) {
            report.setReportType("MANUAL");
        }
        return reportRepository.save(report);
    }

    public Report generateMonthlyFromDailyReports(YearMonth ym) {
        return monthlyReportService.generateFromDailyReports(ym);
    }

    public void finalizeDailyReportAndResetDay(LocalDate day) {
        dailyReportService.finalizeDailyReportAndResetDay(day);
    }

    public List<Report> getAllReports() {
        return reportRepository.findAll();
    }

    public PagedResponse<Report> getReportsPage(String periodType, String search, Integer page, Integer size) {
        int safePage = Math.max(page != null ? page : 0, 0);
        int safeSize = Math.min(Math.max(size != null ? size : 20, 1), 100);
        Page<Report> result = reportRepository.findPageByFilters(
                periodType != null ? periodType.trim() : "",
                search != null ? search.trim() : "",
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "id"))
        );
        return new PagedResponse<>(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.isFirst(), result.isLast());
    }

    public Optional<Report> getReportById(Long id) {
        return reportRepository.findById(id);
    }

    public void deleteReport(Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reporte no encontrado: " + id));

        if (Boolean.TRUE.equals(report.getDailyFinal()) && "DAILY".equalsIgnoreCase(report.getPeriodType())) {
            String monthKey = report.getPeriodKey().substring(0, 7); // yyyy-MM
            if (reportRepository.existsByPeriodTypeAndPeriodKey("MONTHLY", monthKey)) {
                throw new IllegalStateException(
                        "No se puede eliminar el reporte diario final de " + report.getPeriodKey()
                        + " — el reporte mensual de " + monthKey + " ya fue generado con este diario.");
            }
        }

        reportRepository.deleteById(id);
    }

    public List<ClientDTO> getClientsFromDailyReportsByDateRange(LocalDate from, LocalDate to) {
        String fromKey = from.format(DateTimeFormatter.ISO_DATE);
        String toKey = to.format(DateTimeFormatter.ISO_DATE);
        List<Report> reports = reportRepository.findFinalDailyByPeriodKeyRange(fromKey, toKey);

        Set<LocalDate> datesWithReport = new HashSet<>();
        List<ClientDTO> result = new ArrayList<>();

        for (Report r : reports) {
            LocalDate reportDate;
            try {
                reportDate = LocalDate.parse(r.getPeriodKey());
            } catch (Exception e) {
                log.warn("[REPORT-FALLBACK] periodKey inválido id={} key={}", r.getId(), r.getPeriodKey());
                continue;
            }
            datesWithReport.add(reportDate); // marcado aunque filteredClients esté vacío

            if (r.getFilteredClients() == null || r.getFilteredClients().isBlank()) continue;
            try {
                List<Map<String, Object>> clients = mapper.readValue(
                        r.getFilteredClients(), new TypeReference<>() {});
                for (Map<String, Object> m : clients) {
                    ClientDTO dto = ClientDTO.fromMap(m);
                    if (dto != null) result.add(dto);
                }
            } catch (Exception e) {
                log.warn("[REPORT-FALLBACK] Error parseando filteredClients reporte id={} periodKey={}",
                        r.getId(), r.getPeriodKey());
            }
        }

        // Días del rango sin reporte final → segunda capa: ServiceHistory
        Set<LocalDate> gapDates = from.datesUntil(to.plusDays(1))
                .filter(d -> !datesWithReport.contains(d))
                .collect(Collectors.toCollection(HashSet::new));

        if (!gapDates.isEmpty()) {
            LocalDate gapFrom = gapDates.stream().min(Comparator.naturalOrder()).orElse(from);
            LocalDate gapTo   = gapDates.stream().max(Comparator.naturalOrder()).orElse(to);
            List<ServiceHistory> histories = serviceHistoryService.getByDateRange(gapFrom, gapTo);
            int fromHistory = 0;
            for (ServiceHistory h : histories) {
                if (!gapDates.contains(h.getServiceDate())) continue; // evita días cubiertos por reporte
                ClientDTO dto = ClientDTO.fromServiceHistory(h);
                if (dto != null) { result.add(dto); fromHistory++; }
            }
            if (fromHistory > 0) {
                log.info("[REPORT-FALLBACK] {} servicio(s) de ServiceHistory para dias sin reporte: {}",
                        fromHistory, gapDates);
            }
        }

        return result;
    }

    private YearMonth resolveYearMonth(Report report) {
        if (report.getPeriodKey() != null && !report.getPeriodKey().isBlank()) {
            return YearMonth.parse(report.getPeriodKey());
        }
        if (report.getTimestamp() != null && !report.getTimestamp().isBlank()) {
            return YearMonth.from(OffsetDateTime.parse(report.getTimestamp()));
        }
        return YearMonth.now();
    }
}
