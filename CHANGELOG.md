# Changelog

All notable changes to this project are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [0.1.0] - 2026-04-24

### Added

- Initial release.
- TimescaleDB hypertable `ping` (1-hour chunks) with PostGIS `geography(POINT, 4326)`.
- Compression policy (segmentby `h3_r8`) after 2 hours.
- Retention policy: raw pings kept for 7 days.
- Continuous aggregate `hotspot_1min` with `materialized_only = false` for live-blended reads.
- Write-time Uber H3 indexing at resolutions 7, 8, 9.
- REST ingest: `POST /api/v1/pings` and `POST /api/v1/pings/batch` (Jakarta Validation).
- Kafka batch consumer on topic `pings.raw` with `ErrorHandlingDeserializer` + manual offset ack.
- Hotspot query API: `GET /api/v1/hotspots` with time window + optional bbox + mode filter, bbox translated to H3 cell set via `polygonToCells`.
- Live hotspots via Server-Sent Events: `GET /api/v1/live/hotspots` (5 s push, 15 s heartbeat).
- Paris-scale mobility simulator (`mobility.simulator.enabled=true`): 1000 synthetic devices across 6 transport modes, 200 pings/sec default.
- Micrometer + Prometheus metrics under `mobility.ingest.*` with histogram buckets for p50/p95/p99.
- Grafana dashboard (ingest rate, latency percentiles, Kafka throughput, JVM heap).
- Flyway migrations (`V1__init_schema.sql`).
- Testcontainers integration test against `timescale/timescaledb-ha:pg17`.
