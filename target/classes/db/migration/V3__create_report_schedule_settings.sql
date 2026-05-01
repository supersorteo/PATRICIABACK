CREATE TABLE IF NOT EXISTS report_schedule_settings (
    id BIGINT NOT NULL PRIMARY KEY,
    enabled BIT(1) NOT NULL DEFAULT b'0',
    daily_snapshot_time VARCHAR(5) NULL,
    last_snapshot_day DATE NULL,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO report_schedule_settings (id, enabled, daily_snapshot_time, last_snapshot_day)
VALUES (1, b'0', NULL, NULL)
ON DUPLICATE KEY UPDATE id = id;
