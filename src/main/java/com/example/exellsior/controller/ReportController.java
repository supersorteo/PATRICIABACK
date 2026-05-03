package com.example.exellsior.controller;

import com.example.exellsior.dto.PagedResponse;
import com.example.exellsior.dto.ReportScheduleConfigDTO;
import com.example.exellsior.entity.Report;
import com.example.exellsior.services.OperationalTimeService;
import com.example.exellsior.services.ReportScheduleService;
import com.example.exellsior.services.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    @Autowired
    private ReportService reportService;
    @Autowired
    private ReportScheduleService reportScheduleService;
    @Autowired
    private OperationalTimeService operationalTimeService;



    // agrega endpoint manual mensual
    @PostMapping("/monthly/generate")
    public ResponseEntity<Report> generateMonthly(@RequestParam(required = false) String month) {
        YearMonth ym = (month == null || month.isBlank())
                ? YearMonth.now()
                : YearMonth.parse(month); // yyyy-MM
        Report r = reportService.generateMonthlyFromDailyReports(ym);
        return ResponseEntity.ok(r);
    }

    @PostMapping
    public ResponseEntity<Report> createOrGenerate(@RequestBody Report report) {
        Report savedReport = reportService.createOrGenerate(report);
        return ResponseEntity.ok(savedReport);
    }

    @GetMapping("/schedule")
    public ResponseEntity<ReportScheduleConfigDTO> getScheduleConfig() {
        return ResponseEntity.ok(reportScheduleService.getConfig());
    }

    @PutMapping("/schedule")
    public ResponseEntity<ReportScheduleConfigDTO> updateScheduleConfig(@RequestBody ReportScheduleConfigDTO config) {
        return ResponseEntity.ok(reportScheduleService.updateConfig(config));
    }



    @GetMapping
    public ResponseEntity<List<Report>> getAllReports() {
        List<Report> reports = reportService.getAllReports();
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/page")
    public ResponseEntity<PagedResponse<Report>> getReportsPage(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String periodType,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(reportService.getReportsPage(periodType, search, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Report> getReportById(@PathVariable Long id) {
        Optional<Report> report = reportService.getReportById(id);
        return report.map(r -> ResponseEntity.ok(r))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReport(@PathVariable Long id) {
        try {
            reportService.deleteReport(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/method1")
    public ResponseEntity<Void> deleteReportMethod1(@PathVariable Long id) {
        reportService.deleteReportMethod1(id);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/daily/finalize-and-close")
    public ResponseEntity<Void> finalizeDailyAndClose(@RequestParam(required = false) String day) {
        ZoneId zone = operationalTimeService.getBusinessZone();
        LocalDate targetDay = (day == null || day.isBlank())
                ? LocalDate.now(zone)
                : LocalDate.parse(day);

        reportService.finalizeDailyReportAndResetDay(targetDay);
        return ResponseEntity.ok().build();
    }




}
