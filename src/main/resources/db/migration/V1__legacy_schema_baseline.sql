-- Esquema base para instalaciones nuevas sobre una base vacia.
-- Las migraciones posteriores (V2+) agregan indices y extensiones incrementales.

CREATE TABLE IF NOT EXISTS admin_users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    CONSTRAINT uk_admin_users_username UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS clients (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(255) NULL,
    name VARCHAR(255) NOT NULL,
    dni VARCHAR(255) NULL,
    phone_intl VARCHAR(255) NULL,
    phone_raw VARCHAR(255) NULL,
    plate VARCHAR(255) NULL,
    notes VARCHAR(255) NULL,
    space_key VARCHAR(255) NULL,
    vehicle VARCHAR(255) NULL,
    category VARCHAR(255) NULL,
    price INT NULL,
    payment_method VARCHAR(255) NULL,
    clover INT NULL,
    entry_timestamp DATETIME(6) NULL,
    exit_timestamp BIGINT NULL,
    last_day_closed BIGINT NULL
);

CREATE TABLE IF NOT EXISTS reports (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `timestamp` VARCHAR(255) NOT NULL,
    period_type VARCHAR(255) NOT NULL,
    period_key VARCHAR(255) NOT NULL,
    total_spaces INT NOT NULL DEFAULT 0,
    occupied_spaces INT NOT NULL DEFAULT 0,
    free_spaces INT NOT NULL DEFAULT 0,
    occupancy_rate INT NOT NULL DEFAULT 0,
    subsuelo_stats LONGTEXT NULL,
    time_stats LONGTEXT NULL,
    filtered_clients LONGTEXT NULL,
    payment_amounts LONGTEXT NULL,
    total_cobrado BIGINT NULL,
    daily_final BIT(1) NULL DEFAULT b'0',
    report_type VARCHAR(32) NULL
);

CREATE TABLE IF NOT EXISTS subsuelos (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    label VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS spaces (
    space_key VARCHAR(255) NOT NULL PRIMARY KEY,
    subsuelo_id VARCHAR(255) NOT NULL,
    occupied BIT(1) NOT NULL DEFAULT b'0',
    hold BIT(1) NOT NULL DEFAULT b'0',
    client_id BIGINT NULL,
    start_time BIGINT NULL,
    display_name VARCHAR(255) NULL
);

CREATE TABLE IF NOT EXISTS vehicle_types (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    model VARCHAR(255) NOT NULL,
    category VARCHAR(255) NOT NULL,
    price INT NOT NULL,
    CONSTRAINT uk_vehicle_types_model UNIQUE (model)
);

CREATE TABLE IF NOT EXISTS client_vehicles (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    client_id BIGINT NULL,
    vehicle_type_id BIGINT NULL,
    plate VARCHAR(255) NULL,
    notes VARCHAR(255) NULL
);
