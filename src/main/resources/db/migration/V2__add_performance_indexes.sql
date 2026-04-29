-- Indices no destructivos para mejorar consultas operativas y reportes.
-- La migracion es defensiva: solo crea cada indice si todavia no existe.

SET @current_schema = DATABASE();

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = @current_schema
          AND table_name = 'clients'
          AND index_name = 'idx_clients_dni'
    ),
    'SELECT ''idx_clients_dni already exists''',
    'CREATE INDEX idx_clients_dni ON clients (dni)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = @current_schema
          AND table_name = 'clients'
          AND index_name = 'idx_clients_entry_timestamp'
    ),
    'SELECT ''idx_clients_entry_timestamp already exists''',
    'CREATE INDEX idx_clients_entry_timestamp ON clients (entry_timestamp)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = @current_schema
          AND table_name = 'clients'
          AND index_name = 'idx_clients_exit_timestamp'
    ),
    'SELECT ''idx_clients_exit_timestamp already exists''',
    'CREATE INDEX idx_clients_exit_timestamp ON clients (exit_timestamp)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = @current_schema
          AND table_name = 'clients'
          AND index_name = 'idx_clients_space_key'
    ),
    'SELECT ''idx_clients_space_key already exists''',
    'CREATE INDEX idx_clients_space_key ON clients (space_key)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = @current_schema
          AND table_name = 'spaces'
          AND index_name = 'idx_spaces_client_id'
    ),
    'SELECT ''idx_spaces_client_id already exists''',
    'CREATE INDEX idx_spaces_client_id ON spaces (client_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = @current_schema
          AND table_name = 'reports'
          AND index_name = 'idx_reports_period_type_period_key'
    ),
    'SELECT ''idx_reports_period_type_period_key already exists''',
    'CREATE INDEX idx_reports_period_type_period_key ON reports (period_type, period_key)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
