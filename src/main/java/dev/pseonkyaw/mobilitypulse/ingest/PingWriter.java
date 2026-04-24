package dev.pseonkyaw.mobilitypulse.ingest;

import dev.pseonkyaw.mobilitypulse.domain.PingEvent;
import dev.pseonkyaw.mobilitypulse.geo.H3Indexer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.List;

/**
 * Batch-inserts pings into the TimescaleDB hypertable using JdbcTemplate.
 * <p>
 * We deliberately bypass JPA here because the hot path is write-heavy and
 * Hibernate's dirty-tracking overhead is wasted on single-INSERT entities.
 * PostGIS {@code geography(POINT, 4326)} is constructed inline via
 * {@code ST_SetSRID(ST_MakePoint(lon, lat), 4326)}.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class PingWriter {

    private static final String INSERT_SQL = """
            INSERT INTO ping (
                ts, device_id, mode, lat, lon, speed_kmh, heading_deg,
                geo, h3_r7, h3_r8, h3_r9
            ) VALUES (
                ?, ?, ?, ?, ?, ?, ?,
                ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography,
                ?, ?, ?
            )
            """;

    private final JdbcTemplate jdbc;
    private final H3Indexer h3;

    public int writeBatch(List<PingEvent> pings) {
        if (pings.isEmpty()) {
            return 0;
        }
        int[] inserted = jdbc.batchUpdate(INSERT_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                PingEvent p = pings.get(i);
                Instant ts = p.ts() != null ? p.ts() : Instant.now();

                ps.setTimestamp(1, Timestamp.from(ts));
                ps.setString(2, p.deviceId());
                ps.setString(3, p.mode().name());
                ps.setDouble(4, p.lat());
                ps.setDouble(5, p.lon());
                setNullableDouble(ps, 6, p.speedKmh());
                setNullableInt(ps, 7, p.headingDeg());

                ps.setDouble(8, p.lon());
                ps.setDouble(9, p.lat());

                ps.setLong(10, h3.cellAt(p.lat(), p.lon(), H3Indexer.RES_CITY));
                ps.setLong(11, h3.cellAt(p.lat(), p.lon(), H3Indexer.RES_NEIGHBOURHOOD));
                ps.setLong(12, h3.cellAt(p.lat(), p.lon(), H3Indexer.RES_BLOCK));
            }

            @Override
            public int getBatchSize() {
                return pings.size();
            }
        });

        int total = 0;
        for (int r : inserted) {
            if (r >= 0) {
                total += r;
            }
        }
        return total;
    }

    private static void setNullableDouble(PreparedStatement ps, int idx, Double v) throws SQLException {
        if (v == null) {
            ps.setNull(idx, Types.DOUBLE);
        } else {
            ps.setDouble(idx, v);
        }
    }

    private static void setNullableInt(PreparedStatement ps, int idx, Integer v) throws SQLException {
        if (v == null) {
            ps.setNull(idx, Types.INTEGER);
        } else {
            ps.setInt(idx, v);
        }
    }
}
