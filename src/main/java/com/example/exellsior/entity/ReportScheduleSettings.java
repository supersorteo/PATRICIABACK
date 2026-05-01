package com.example.exellsior.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "report_schedule_settings")
public class ReportScheduleSettings {

    @Id
    private Long id;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(name = "daily_snapshot_time", length = 5)
    private String dailySnapshotTime;

    @Column(name = "last_snapshot_day")
    private LocalDate lastSnapshotDay;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDailySnapshotTime() {
        return dailySnapshotTime;
    }

    public void setDailySnapshotTime(String dailySnapshotTime) {
        this.dailySnapshotTime = dailySnapshotTime;
    }

    public LocalDate getLastSnapshotDay() {
        return lastSnapshotDay;
    }

    public void setLastSnapshotDay(LocalDate lastSnapshotDay) {
        this.lastSnapshotDay = lastSnapshotDay;
    }
}
