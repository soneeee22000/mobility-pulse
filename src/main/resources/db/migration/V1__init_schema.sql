-- ============================================================
-- mobility-pulse — initial TimescaleDB + PostGIS schema
-- ============================================================

CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE;
CREATE EXTENSION IF NOT EXISTS postgis CASCADE;

-- ------------------------------------------------------------
-- Raw ping — hypertable partitioned by time
-- ------------------------------------------------------------
CREATE TABLE ping (
    ts          TIMESTAMPTZ      NOT NULL,
    device_id   VARCHAR(64)      NOT NULL,
    mode        VARCHAR(16)      NOT NULL,
    lat         DOUBLE PRECISION NOT NULL,
    lon         DOUBLE PRECISION NOT NULL,
    speed_kmh   DOUBLE PRECISION,
    heading_deg INTEGER,
    geo         GEOGRAPHY(POINT, 4326) NOT NULL,
    h3_r7       BIGINT           NOT NULL,
    h3_r8       BIGINT           NOT NULL,
    h3_r9       BIGINT           NOT NULL
);

SELECT create_hypertable('ping', 'ts', chunk_time_interval := INTERVAL '1 hour');

CREATE INDEX idx_ping_device_ts ON ping (device_id, ts DESC);
CREATE INDEX idx_ping_h3r8_ts   ON ping (h3_r8, ts DESC);
CREATE INDEX idx_ping_h3r9_ts   ON ping (h3_r9, ts DESC);
CREATE INDEX idx_ping_geo_gist  ON ping USING GIST (geo);

-- ------------------------------------------------------------
-- Compression — compress chunks older than 2 hours
--   segmentby h3_r8 keeps per-hex queries fast on compressed chunks
-- ------------------------------------------------------------
ALTER TABLE ping SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'h3_r8',
    timescaledb.compress_orderby   = 'ts DESC'
);
SELECT add_compression_policy('ping', INTERVAL '2 hours');

-- ------------------------------------------------------------
-- Retention — raw pings live for 7 days
-- ------------------------------------------------------------
SELECT add_retention_policy('ping', INTERVAL '7 days');

-- ------------------------------------------------------------
-- Continuous aggregate — 1-minute hotspot rollup per (h3_r8, mode)
--   materialized_only = false → queries blend materialized rollups
--   with sub-minute real-time data from the hypertable
-- ------------------------------------------------------------
CREATE MATERIALIZED VIEW hotspot_1min
    WITH (timescaledb.continuous, timescaledb.materialized_only = false) AS
SELECT
    time_bucket(INTERVAL '1 minute', ts) AS bucket,
    h3_r8,
    mode,
    COUNT(*)         AS ping_count,
    AVG(speed_kmh)   AS avg_speed_kmh
FROM ping
GROUP BY bucket, h3_r8, mode
WITH NO DATA;

SELECT add_continuous_aggregate_policy('hotspot_1min',
    start_offset      := INTERVAL '1 hour',
    end_offset        := INTERVAL '30 seconds',
    schedule_interval := INTERVAL '30 seconds'
);

CREATE INDEX idx_hotspot_bucket ON hotspot_1min (bucket DESC, h3_r8);
