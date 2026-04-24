package dev.pseonkyaw.mobilitypulse.query;

import dev.pseonkyaw.mobilitypulse.api.BoundingBox;
import dev.pseonkyaw.mobilitypulse.api.HotspotCell;
import dev.pseonkyaw.mobilitypulse.domain.TransportMode;
import dev.pseonkyaw.mobilitypulse.geo.BboxResolver;
import dev.pseonkyaw.mobilitypulse.geo.H3Indexer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HotspotService {

    private static final int DEFAULT_LIMIT = 2000;
    private static final int HARD_LIMIT = 10_000;

    private final HotspotRepository repository;
    private final BboxResolver bboxResolver;

    public List<HotspotCell> hotspots(
            Instant from,
            Instant to,
            BoundingBox bbox,
            TransportMode mode,
            Integer limit
    ) {
        int effectiveLimit = Math.min(limit == null ? DEFAULT_LIMIT : limit, HARD_LIMIT);
        List<Long> cells = bbox == null ? List.of()
                : bboxResolver.cellsCoveringBbox(bbox, H3Indexer.RES_NEIGHBOURHOOD);
        return repository.query(from, to, cells, mode, effectiveLimit);
    }

    public List<HotspotCell> recentHotspots(Duration window, BoundingBox bbox, TransportMode mode, int limit) {
        Instant now = Instant.now();
        return hotspots(now.minus(window), now, bbox, mode, limit);
    }
}
