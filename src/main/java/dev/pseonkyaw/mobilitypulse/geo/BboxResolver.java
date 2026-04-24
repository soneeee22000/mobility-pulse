package dev.pseonkyaw.mobilitypulse.geo;

import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;
import dev.pseonkyaw.mobilitypulse.api.BoundingBox;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Converts a geographic bounding box into the set of H3 cells that cover it
 * at a target resolution. We push this cell-id set into the SQL query as a
 * {@code BIGINT[]} to enable a cheap index scan on {@code h3_r8} rather than
 * a geographic distance filter on the cagg.
 */
@Slf4j
@Component
public class BboxResolver {

    private H3Core h3;

    @PostConstruct
    void init() {
        try {
            this.h3 = H3Core.newInstance();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<Long> cellsCoveringBbox(BoundingBox bbox, int resolution) {
        List<LatLng> polygon = List.of(
                new LatLng(bbox.south(), bbox.west()),
                new LatLng(bbox.south(), bbox.east()),
                new LatLng(bbox.north(), bbox.east()),
                new LatLng(bbox.north(), bbox.west())
        );
        return h3.polygonToCells(polygon, List.of(), resolution);
    }
}
