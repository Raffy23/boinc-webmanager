--
-- Initialization SQL file for the BOINC Webmanager Database
--

-- Store all auto-discovered core clients persistently in this table
CREATE TABLE IF NOT EXISTS core_client (
    name       VARCHAR(64) PRIMARY KEY,
    ip_address VARCHAR(16),
);