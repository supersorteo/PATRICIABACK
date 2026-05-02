CREATE TABLE service_history (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    service_key VARCHAR(255) NOT NULL,
    source_client_id BIGINT NULL,
    code VARCHAR(255) NULL,
    name VARCHAR(255) NOT NULL,
    dni VARCHAR(255) NULL,
    phone_intl VARCHAR(255) NULL,
    phone_raw VARCHAR(255) NULL,
    plate VARCHAR(255) NULL,
    notes TEXT NULL,
    space_key VARCHAR(255) NULL,
    vehicle VARCHAR(255) NULL,
    category VARCHAR(255) NULL,
    price INT NULL,
    payment_method VARCHAR(255) NULL,
    clover INT NULL,
    entry_timestamp_ms BIGINT NULL,
    exit_timestamp_ms BIGINT NOT NULL,
    service_date DATE NOT NULL,
    archived_at BIGINT NOT NULL,
    archived_by VARCHAR(64) NOT NULL
);

ALTER TABLE service_history
    ADD CONSTRAINT uk_service_history_service_key UNIQUE (service_key);

CREATE INDEX idx_service_history_service_date
    ON service_history (service_date);

CREATE INDEX idx_service_history_exit_ts
    ON service_history (exit_timestamp_ms);

CREATE INDEX idx_service_history_source_client
    ON service_history (source_client_id);
