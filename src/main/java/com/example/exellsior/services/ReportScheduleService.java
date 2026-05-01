package com.example.exellsior.services;

import com.example.exellsior.dto.ReportScheduleConfigDTO;
import com.example.exellsior.entity.Report;
import com.example.exellsior.entity.ReportScheduleSettings;
import com.example.exellsior.repository.ReportScheduleSettingsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

@Slf4j
@Service
public class ReportScheduleService {

    private static final Long SETTINGS_ID = 1L;
    private static final ZoneId REPORT_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    private ReportScheduleSettingsRepository settingsRepository;

    @Autowired
    private DailyReportService dailyReportService;

    @Transactional(readOnly = true)
    public ReportScheduleConfigDTO getConfig() {
        return ReportScheduleConfigDTO.from(getOrCreateSettings());
    }

    @Transactional
    public ReportScheduleConfigDTO updateConfig(ReportScheduleConfigDTO dto) {
        ReportScheduleSettings settings = getOrCreateSettings();
        String previousTime = settings.getDailySnapshotTime();
        boolean previousEnabled = settings.isEnabled();

        String normalizedTime = normalizeTime(dto.dailySnapshotTime());
        boolean enabled = dto.enabled() && normalizedTime != null;
        LocalDate today = LocalDate.now(REPORT_ZONE);
        LocalTime nowTime = LocalTime.now(REPORT_ZONE).withSecond(0).withNano(0);
        LocalTime configuredTime = parseConfiguredTime(normalizedTime);

        settings.setDailySnapshotTime(normalizedTime);
        settings.setEnabled(enabled);
        if (!enabled) {
            settings.setLastSnapshotDay(null);
        } else if (dto.lastSnapshotDay() == null
                || !Objects.equals(previousTime, normalizedTime)
                || (!previousEnabled && enabled)) {
            settings.setLastSnapshotDay(shouldWaitUntilTomorrow(nowTime, configuredTime) ? today : null);
        } else {
            settings.setLastSnapshotDay(dto.lastSnapshotDay());
        }

        ReportScheduleSettings saved = settingsRepository.save(settings);
        log.info("[REPORT-SCHEDULE] Config actualizada enabled={} time={} lastSnapshotDay={}",
                saved.isEnabled(), saved.getDailySnapshotTime(), saved.getLastSnapshotDay());
        return ReportScheduleConfigDTO.from(saved);
    }

    @Scheduled(cron = "0 * * * * *", zone = "America/Argentina/Buenos_Aires")
    public void runScheduledSnapshotCheck() {
        runScheduledSnapshotIfNeeded("SCHEDULED");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void catchUpOnStartup() {
        runScheduledSnapshotIfNeeded("STARTUP");
    }

    @Transactional
    protected void runScheduledSnapshotIfNeeded(String source) {
        ReportScheduleSettings settings = getOrCreateSettings();
        if (!settings.isEnabled()) {
            return;
        }

        LocalTime configuredTime = parseConfiguredTime(settings.getDailySnapshotTime());
        if (configuredTime == null) {
            log.warn("[REPORT-SCHEDULE] Hora invalida o ausente. Se desactiva el scheduler.");
            settings.setEnabled(false);
            settingsRepository.save(settings);
            return;
        }

        LocalDate today = LocalDate.now(REPORT_ZONE);
        LocalTime nowTime = LocalTime.now(REPORT_ZONE).withSecond(0).withNano(0);

        boolean timeReached = !nowTime.isBefore(configuredTime);
        boolean alreadyGeneratedToday = today.equals(settings.getLastSnapshotDay());

        if (!timeReached || alreadyGeneratedToday) {
            return;
        }

        try {
            Report report = dailyReportService.upsertDailySnapshotForDay(today);
            settings.setLastSnapshotDay(today);
            settingsRepository.save(settings);
            log.info("[REPORT-SCHEDULE] source={} day={} reportId={}",
                    source, today, report != null ? report.getId() : "null");
        } catch (Exception ex) {
            log.error("[REPORT-SCHEDULE] Error generando snapshot diario programado", ex);
        }
    }

    private ReportScheduleSettings getOrCreateSettings() {
        return settingsRepository.findById(SETTINGS_ID).orElseGet(() -> {
            ReportScheduleSettings settings = new ReportScheduleSettings();
            settings.setId(SETTINGS_ID);
            settings.setEnabled(false);
            settings.setDailySnapshotTime(null);
            settings.setLastSnapshotDay(null);
            return settingsRepository.save(settings);
        });
    }

    private String normalizeTime(String rawTime) {
        if (rawTime == null || rawTime.isBlank()) {
            return null;
        }

        try {
            return LocalTime.parse(rawTime.trim(), TIME_FORMATTER).format(TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hora programada invalida. Use formato HH:mm");
        }
    }

    private LocalTime parseConfiguredTime(String rawTime) {
        if (rawTime == null || rawTime.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(rawTime, TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private boolean shouldWaitUntilTomorrow(LocalTime nowTime, LocalTime configuredTime) {
        if (nowTime == null || configuredTime == null) {
            return false;
        }
        return !configuredTime.isAfter(nowTime);
    }
}
