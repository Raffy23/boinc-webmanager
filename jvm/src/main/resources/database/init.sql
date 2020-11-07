--
-- Initialization SQL file for the BOINC Webmanager Database
--

-- Store all auto-discovered core clients persistently in this table
CREATE TABLE IF NOT EXISTS core_client (
    name        VARCHAR(64) PRIMARY KEY,        -- Usually the hostname, but can be set freely
    address     VARCHAR(256),                   -- max length of FQDN, dns or ip can be used
    port        INTEGER,                        -- the port of the core client
    password    VARCHAR(256),                   -- the password, must be saved in clear text since
                                                -- hashing must be done when authenticating
    added_by    ENUM('user', 'discovery')       -- reason to why this entry was created
);

-- Additional projects that can be added to the Webmanager
CREATE TABLE IF NOT EXISTS project (
    name            VARCHAR(64),
    url             VARCHAR(256) PRIMARY KEY,
    general_area    TEXT,
    specific_area   TEXT,
    description     TEXT,
    home            TEXT,
    platforms       ARRAY                         --
);