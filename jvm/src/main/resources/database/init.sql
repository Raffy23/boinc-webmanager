--
-- Initialization SQL file for the BOINC Webmanager Database
--

-- Store all auto-discovered core clients persistently in this table
CREATE TABLE IF NOT EXISTS core_client (
    name        VARCHAR(64) PRIMARY KEY,
    ip_address  VARCHAR(16) UNIQUE,
    password    VARCHAR(256)
);

-- Additional projects that can be added to the Webmanager
CREATE TABLE IF NOT EXISTS project (
    name            VARCHAR(64),
    url             VARCHAR(256) PRIMARY KEY,
    general_area    TEXT,
    specific_area   TEXT,
    description     TEXT,
    home            TEXT,
    platforms       TEXT -- ; separated list of entries
);