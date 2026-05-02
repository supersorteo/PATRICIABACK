package com.example.exellsior.services;

import com.example.exellsior.dto.ClientDTO;
import com.example.exellsior.dto.PagedResponse;
import com.example.exellsior.entity.Report;
import com.example.exellsior.repository.ReportRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class ReportService {

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired private ReportRepository reportRepository;
    @Autowired private DailyReportService dailyReportService;
    @Autowired private MonthlyReportService monthlyReportService;

    public Report saveReport(Report report) {
        if (report.getPeriodType() == null || report.getPeriodType().isBlank()) {
            report.setPeriodType("DAILY");
        }
        if (report.getPeriodKey() == null || report.getPeriodKey().isBlank()) {
            report.setPeriodKey(LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires"))
                    .format(DateTimeFormatter.ISO_DATE));
        }
        if (report.getTimestamp() == null || report.getTimestamp().isBlank()) {
            report.setTimestamp(OffsetDateTime.now(ZoneId.of("America/Argentina/Buenos_Aires")).toString());
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
            report.setPeriodKey(LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires")).toString());
        }
        if (report.getTimestamp() == null || report.getTimestamp().isBlank()) {
            report.setTimestamp(OffsetDateTime.now(ZoneId.of("America/Argentina/Buenos_Aires")).toString());
        }
        if (report.getDailyFinal() == null) {
            report.setDailyFinal(false);
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
        List<ClientDTO> result = new ArrayList<>();
        for (Report r : reports) {
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
