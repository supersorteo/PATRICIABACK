ALTER TABLE report_schedule_settings
    ADD COLUMN business_time_zone VARCHAR(64) NULL AFTER daily_snapshot_time,
    ADD COLUMN daily_close_time VARCHAR(5) NULL AFTER business_time_zone,
    ADD COLUMN last_close_day DATE NULL AFTER last_snapshot_day;

UPDATE report_schedule_settings
SET business_time_zone = COALESCE(NULLIF(TRIM(business_time_zone), ''), 'America/Havana'),
    daily_close_time = COALESCE(NULLIF(TRIM(daily_close_time), ''), '23:59')
WHERE id = 1;
