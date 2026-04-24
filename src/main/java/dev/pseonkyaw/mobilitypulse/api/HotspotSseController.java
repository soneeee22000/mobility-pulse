package dev.pseonkyaw.mobilitypulse.api;

import dev.pseonkyaw.mobilitypulse.domain.TransportMode;
import dev.pseonkyaw.mobilitypulse.query.HotspotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Server-Sent Events stream of live hotspots. Clients subscribe with
 * {@code curl -N http://localhost:8080/api/v1/live/hotspots}. The server
 * pushes the last 2-minute hotspot snapshot every 5 seconds and a heartbeat
 * comment every 15 seconds to keep middleware-level idle timeouts at bay.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/live/hotspots")
@RequiredArgsConstructor
public class HotspotSseController {

    private static final long CLIENT_TIMEOUT_MS = 30 * 60 * 1000L;
    private static final Duration DEFAULT_WINDOW = Duration.ofMinutes(2);
    private static final int MAX_CELLS_PER_PUSH = 500;

    private final HotspotService hotspotService;
    private final List<Subscription> subs = new CopyOnWriteArrayList<>();

    @GetMapping
    public SseEmitter stream(
            @RequestParam(required = false) Double south,
            @RequestParam(required = false) Double west,
            @RequestParam(required = false) Double north,
            @RequestParam(required = false) Double east,
            @RequestParam(required = false) TransportMode mode
    ) {
        SseEmitter emitter = new SseEmitter(CLIENT_TIMEOUT_MS);
        BoundingBox bbox = null;
        if (south != null && west != null && north != null && east != null) {
            bbox = new BoundingBox(south, west, north, east);
        }
        Subscription sub = new Subscription(emitter, bbox, mode);
        subs.add(sub);

        emitter.onCompletion(() -> subs.remove(sub));
        emitter.onTimeout(() -> subs.remove(sub));
        emitter.onError(err -> subs.remove(sub));

        try {
            emitter.send(SseEmitter.event().name("ready").data("connected"));
        } catch (IOException e) {
            subs.remove(sub);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    @Scheduled(fixedDelay = 5000L, initialDelay = 5000L)
    public void pushHotspots() {
        if (subs.isEmpty()) {
            return;
        }
        for (Subscription sub : subs) {
            try {
                List<HotspotCell> snapshot = hotspotService.recentHotspots(
                        DEFAULT_WINDOW, sub.bbox(), sub.mode(), MAX_CELLS_PER_PUSH
                );
                sub.emitter().send(SseEmitter.event().name("hotspots").data(snapshot));
            } catch (Exception ex) {
                log.debug("SSE send failed, dropping subscriber: {}", ex.getMessage());
                subs.remove(sub);
                sub.emitter().completeWithError(ex);
            }
        }
    }

    @Scheduled(fixedDelay = 15000L, initialDelay = 15000L)
    public void heartbeat() {
        for (Subscription sub : subs) {
            try {
                sub.emitter().send(SseEmitter.event().comment("hb"));
            } catch (Exception ex) {
                subs.remove(sub);
                sub.emitter().completeWithError(ex);
            }
        }
    }

    private record Subscription(SseEmitter emitter, BoundingBox bbox, TransportMode mode) {}
}
