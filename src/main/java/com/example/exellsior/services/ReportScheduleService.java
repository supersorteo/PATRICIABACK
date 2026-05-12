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

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Objects;

@Slf4j
@Service
public class ReportScheduleService {

    @Autowired
    private DailyReportService dailyReportService;

    @Autowired
    private OperationalTimeService operationalTimeService;

    @Autowired
    private ReportScheduleSettingsRepository settingsRepository;

    @Transactional(readOnly = true)
    public ReportScheduleConfigDTO getConfig() {
        ReportScheduleSettings settings = operationalTimeService.getOrCreateSettings();
        return new ReportScheduleConfigDTO(
                settings.isEnabled(),
                settings.getDailySnapshotTime(),
                operationalTimeService.getBusinessTimeZoneId(),
                settings.getDailyCloseTime(),
                settings.getLastSnapshotDay(),
                settings.getLastCloseDay()
        );
    }

    @Transactional
    public ReportScheduleConfigDTO updateConfig(ReportScheduleConfigDTO dto) {
        ReportScheduleSettings settings = operationalTimeService.getOrCreateSettings();
        String previousTime = settings.getDailySnapshotTime();
        boolean previousEnabled = settings.isEnabled();
        String previousZone = settings.getBusinessTimeZone();
        String previousCloseTime = settings.getDailyCloseTime();

        String normalizedTime = operationalTimeService.normalizeTimeOrNull(dto.dailySnapshotTime());
        boolean enabled = dto.enabled() && normalizedTime != null;
        String normalizedZone = operationalTimeService.normalizeBusinessTimeZone(dto.businessTimeZone());
        String normalizedCloseTime = operationalTimeService.normalizeTimeOrDefault(
                dto.dailyCloseTime(),
                settings.getDailyCloseTime()
        );
        // Solo sobreescribir si el frontend envió un timezone válido; preservar el de BD si llega null.
        if (normalizedZone != null) {
            settings.setBusinessTimeZone(normalizedZone);
        }
        ZoneId effectiveZone = ZoneId.of(operationalTimeService.getBusinessTimeZoneId());
        LocalDate today = LocalDate.now(effectiveZone);
        LocalTime nowTime = LocalTime.now(effectiveZone).withSecond(0).withNano(0);
        LocalTime configuredTime = normalizedTime != null
                ? LocalTime.parse(normalizedTime, OperationalTimeService.TIME_FORMATTER)
                : null;

        settings.setDailySnapshotTime(normalizedTime);
        settings.setEnabled(enabled);
        settings.setDailyCloseTime(normalizedCloseTime);
        if (dto.lastCloseDay() == null || !Objects.equals(previousCloseTime, normalizedCloseTime)) {
            settings.setLastCloseDay(null);
        } else {
            settings.setLastCloseDay(dto.lastCloseDay());
        }
        if (!enabled) {
            settings.setLastSnapshotDay(null);
        } else if (dto.lastSnapshotDay() == null
                || !Objects.equals(previousTime, normalizedTime)
                || !Objects.equals(previousZone, normalizedZone)
                || (!previousEnabled && enabled)) {
            settings.setLastSnapshotDay(shouldWaitUntilTomorrow(nowTime, configuredTime) ? today : null);
        } else {
            settings.setLastSnapshotDay(dto.lastSnapshotDay());
        }

        ReportScheduleSettings finalSaved = settingsRepository.save(settings);
        log.info("[REPORT-SCHEDULE] Config actualizada enabled={} time={} zone={} closeTime={} lastSnapshotDay={} lastCloseDay={}",
                finalSaved.isEnabled(),
                finalSaved.getDailySnapshotTime(),
                finalSaved.getBusinessTimeZone(),
                finalSaved.getDailyCloseTime(),
                finalSaved.getLastSnapshotDay(),
                finalSaved.getLastCloseDay());
        return new ReportScheduleConfigDTO(
                finalSaved.isEnabled(),
                finalSaved.getDailySnapshotTime(),
                operationalTimeService.getBusinessTimeZoneId(),
                finalSaved.getDailyCloseTime(),
                finalSaved.getLastSnapshotDay(),
                finalSaved.getLastCloseDay()
        );
    }

    @Scheduled(cron = "0 * * * * *")
    public void runScheduledSnapshotCheck() {
        runScheduledSnapshotIfNeeded("SCHEDULED");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void catchUpOnStartup() {
        runScheduledSnapshotIfNeeded("STARTUP");
    }

    @Transactional
    protected void runScheduledSnapshotIfNeeded(String source) {
        ReportScheduleSettings settings = operationalTimeService.getOrCreateSettings();
        if (!settings.isEnabled()) {
            return;
        }

        String normalizedSnapshotTime = operationalTimeService.normalizeTimeOrNull(settings.getDailySnapshotTime());
        if (normalizedSnapshotTime == null) {
            log.warn("[REPORT-SCHEDULE] Hora invalida o ausente. Se desactiva el scheduler.");
            settings.setEnabled(false);
            settingsRepository.save(settings);
            return;
        }
        LocalTime configuredTime = LocalTime.parse(normalizedSnapshotTime, OperationalTimeService.TIME_FORMATTER);

        LocalDate today = LocalDate.now(operationalTimeService.getBusinessZone());
        LocalTime nowTime = LocalTime.now(operationalTimeService.getBusinessZone()).withSecond(0).withNano(0);

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

    private boolean shouldWaitUntilTomorrow(LocalTime nowTime, LocalTime configuredTime) {
        if (nowTime == null || configuredTime == null) {
            return false;
        }
        return !configuredTime.isAfter(nowTime);
    }
}
