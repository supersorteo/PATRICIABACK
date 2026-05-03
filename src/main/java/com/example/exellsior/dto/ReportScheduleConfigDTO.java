package com.example.exellsior.dto;

import com.example.exellsior.entity.ReportScheduleSettings;

import java.time.LocalDate;

public record ReportScheduleConfigDTO(
        boolean enabled,
        String dailySnapshotTime,
        String businessTimeZone,
        String dailyCloseTime,
        LocalDate lastSnapshotDay,
        LocalDate lastCloseDay
) {
    public static ReportScheduleConfigDTO from(ReportScheduleSettings settings) {
        return new ReportScheduleConfigDTO(
                settings != null && settings.isEnabled(),
                settings != null ? settings.getDailySnapshotTime() : null,
                settings != null ? settings.getBusinessTimeZone() : null,
                settings != null ? settings.getDailyCloseTime() : null,
                settings != null ? settings.getLastSnapshotDay() : null,
                settings != null ? settings.getLastCloseDay() : null
        );
    }
}
