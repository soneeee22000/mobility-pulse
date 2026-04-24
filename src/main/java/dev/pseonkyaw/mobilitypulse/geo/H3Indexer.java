package dev.pseonkyaw.mobilitypulse.geo;

import com.uber.h3core.H3Core;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Wrapper around Uber H3 for the three resolutions we materialize at ingest:
 * <ul>
 *   <li>r7  — ~5 km² cells — city / borough roll-ups</li>
 *   <li>r8  — ~0.7 km² cells — neighbourhood heatmaps (primary)</li>
 *   <li>r9  — ~0.1 km² cells — block-level drill-downs</li>
 * </ul>
 * Indexing at write time is a deliberate trade-off: we pay 3 × O(1) CPU on the
 * hot path so the read path can hit a plain btree on a single BIGINT column.
 */
@Slf4j
@Component
public class H3Indexer {

    public static final int RES_CITY = 7;
    public static final int RES_NEIGHBOURHOOD = 8;
    public static final int RES_BLOCK = 9;

    private H3Core h3;

    @PostConstruct
    void init() {
        try {
            this.h3 = H3Core.newInstance();
            log.info("H3 core initialised");
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load native h3 library", e);
        }
    }

    public long cellAt(double lat, double lon, int res) {
        return h3.latLngToCell(lat, lon, res);
    }

    public String cellToString(long cell) {
        return h3.h3ToString(cell);
    }

    public long stringToCell(String cell) {
        return h3.stringToH3(cell);
    }

    public int resolutionOf(long cell) {
        return h3.getResolution(cell);
    }
}
