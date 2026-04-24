package dev.pseonkyaw.mobilitypulse.query;

import dev.pseonkyaw.mobilitypulse.api.HotspotCell;
import dev.pseonkyaw.mobilitypulse.domain.TransportMode;
import dev.pseonkyaw.mobilitypulse.geo.H3Indexer;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the {@code hotspot_1min} continuous aggregate. Because the cagg is
 * created with {@code timescaledb.materialized_only = false}, queries blend
 * pre-materialized 1-minute rollups with live data from the underlying
 * hypertable — so the API always returns up-to-the-second hotspots even
 * between refresh cycles.
 */
@Repository
@RequiredArgsConstructor
public class HotspotRepository {

    private final JdbcTemplate jdbc;
    private final H3Indexer h3;

    public List<HotspotCell> query(
            Instant from,
            Instant to,
            List<Long> h3Cells,
            TransportMode modeFilter,
            int limit
    ) {
        StringBuilder sql = new StringBuilder("""
                SELECT bucket, h3_r8, mode, ping_count, avg_speed_kmh
                FROM hotspot_1min
                WHERE bucket >= ?
                  AND bucket <  ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(Timestamp.from(from));
        args.add(Timestamp.from(to));

        if (h3Cells != null && !h3Cells.isEmpty()) {
            sql.append(" AND h3_r8 = ANY(?)\n");
            args.add(h3Cells.stream().mapToLong(Long::longValue).toArray());
        }
        if (modeFilter != null) {
            sql.append(" AND mode = ?\n");
            args.add(modeFilter.name());
        }

        sql.append(" ORDER BY bucket DESC, ping_count DESC\n");
        sql.append(" LIMIT ?");
        args.add(limit);

        RowMapper<HotspotCell> mapper = (rs, rowNum) -> new HotspotCell(
                rs.getTimestamp("bucket").toInstant(),
                h3.cellToString(rs.getLong("h3_r8")),
                TransportMode.valueOf(rs.getString("mode")),
                rs.getLong("ping_count"),
                (Double) rs.getObject("avg_speed_kmh")
        );

        return jdbc.query(sql.toString(), mapper, args.toArray());
    }
}
