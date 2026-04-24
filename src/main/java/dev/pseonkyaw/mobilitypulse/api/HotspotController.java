package dev.pseonkyaw.mobilitypulse.api;

import dev.pseonkyaw.mobilitypulse.domain.TransportMode;
import dev.pseonkyaw.mobilitypulse.query.HotspotService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/hotspots")
@RequiredArgsConstructor
public class HotspotController {

    private final HotspotService hotspotService;

    @GetMapping
    public List<HotspotCell> hotspots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Double south,
            @RequestParam(required = false) Double west,
            @RequestParam(required = false) Double north,
            @RequestParam(required = false) Double east,
            @RequestParam(required = false) TransportMode mode,
            @RequestParam(required = false) Integer limit
    ) {
        BoundingBox bbox = null;
        if (south != null && west != null && north != null && east != null) {
            bbox = new BoundingBox(south, west, north, east);
        }
        return hotspotService.hotspots(from, to, bbox, mode, limit);
    }
}
