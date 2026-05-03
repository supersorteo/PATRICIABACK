UPDATE report_schedule_settings
SET business_time_zone = NULL
WHERE id = 1
  AND business_time_zone = 'America/Havana';
