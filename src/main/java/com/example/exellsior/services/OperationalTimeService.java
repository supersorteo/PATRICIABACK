package com.example.exellsior.services;

import com.example.exellsior.entity.ReportScheduleSettings;
import com.example.exellsior.repository.ReportScheduleSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate; 
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
public class OperationalTimeService {

    public static final Long SETTINGS_ID = 1L;
    public static final String DEFAULT_DAILY_CLOSE_TIME = "23:59";
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Value("${app.business.timezone:America/Argentina/Buenos_Aires}")
    private String configuredDefaultTimezone;

    @Autowired
    private ReportScheduleSettingsRepository settingsRepository;

    @Transactional
    public ReportScheduleSettings getOrCreateSettings() {
        return settingsRepository.findById(SETTINGS_ID).orElseGet(() -> {
            ReportScheduleSettings settings = new ReportScheduleSettings();
            settings.setId(SETTINGS_ID);
            settings.setEnabled(false);
            settings.setDailySnapshotTime(null);
            settings.setLastSnapshotDay(null);
            settings.setBusinessTimeZone(getDefaultBusinessTimeZoneId());
            settings.setDailyCloseTime(DEFAULT_DAILY_CLOSE_TIME);
            settings.setLastCloseDay(null);
            return settingsRepository.save(settings);
        });
    }

    /**
     * Corrige el timezone en BD si quedó en null, vacío, o "UTC" (timezone del servidor cloud).
     * Se llama al arrancar la aplicación para migrar configuraciones existentes.
     */
    @Transactional
    public void fixTimezoneIfNeeded() {
        settingsRepository.findById(SETTINGS_ID).ifPresent(settings -> {
            String tz = settings.getBusinessTimeZone();
            boolean needsFix = tz == null || tz.isBlank() || "UTC".equals(tz);
            if (needsFix) {
                settings.setBusinessTimeZone(getDefaultBusinessTimeZoneId());
                settingsRepository.save(settings);
            }
        });
    }

    @Transactional(readOnly = true)
    public ZoneId getBusinessZone() {
        return resolveBusinessZone(getOrCreateSettings().getBusinessTimeZone());
    }

    @Transactional(readOnly = true)
    public String getBusinessTimeZoneId() {
        return resolveBusinessZone(getOrCreateSettings().getBusinessTimeZone()).getId();
    }

    @Transactional(readOnly = true)
    public LocalTime getDailyCloseTime() {
        return parseTimeOrDefault(getOrCreateSettings().getDailyCloseTime(), DEFAULT_DAILY_CLOSE_TIME);
    }

    @Transactional(readOnly = true)
    public LocalDate getLastCloseDay() {
        return getOrCreateSettings().getLastCloseDay();
    }

    @Transactional
    public void markLastCloseDay(LocalDate day) {
        ReportScheduleSettings settings = getOrCreateSettings();
        settings.setLastCloseDay(day);
        settingsRepository.save(settings);
    }

    @Transactional
    public void clearLastCloseDay() {
        ReportScheduleSettings settings = getOrCreateSettings();
        settings.setLastCloseDay(null);
        settingsRepository.save(settings);
    }

    @Transactional
    public void clearLastCloseDayIfMatches(LocalDate day) {
        ReportScheduleSettings settings = getOrCreateSettings();
        if (day != null && day.equals(settings.getLastCloseDay())) {
            settings.setLastCloseDay(null);
            settingsRepository.save(settings);
        }
    }

    public String normalizeBusinessTimeZone(String rawTimeZone) {
        String candidate = rawTimeZone == null ? "" : rawTimeZone.trim();
        if (candidate.isEmpty()) {
            return null;
        }
        return resolveBusinessZone(candidate).getId();
    }

    public String normalizeTimeOrNull(String rawTime) {
        if (rawTime == null || rawTime.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(rawTime.trim(), TIME_FORMATTER).format(TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    public String normalizeTimeOrDefault(String rawTime, String fallback) {
        String normalized = normalizeTimeOrNull(rawTime);
        if (normalized != null) {
            return normalized;
        }
        return normalizeTimeOrNull(fallback) != null ? normalizeTimeOrNull(fallback) : DEFAULT_DAILY_CLOSE_TIME;
    }

    public LocalTime parseTimeOrDefault(String rawTime, String fallback) {
        String normalized = normalizeTimeOrDefault(rawTime, fallback);
        return LocalTime.parse(normalized, TIME_FORMATTER);
    }

    private ZoneId resolveBusinessZone(String rawTimeZone) {
        String candidate = rawTimeZone == null || rawTimeZone.isBlank()
                ? getDefaultBusinessTimeZoneId()
                : rawTimeZone.trim();
        try {
            return ZoneId.of(candidate);
        } catch (Exception ignored) {
            return ZoneId.systemDefault();
        }
    }

    private String getDefaultBusinessTimeZoneId() {
        return configuredDefaultTimezone;
    }
}
