package dev.pseonkyaw.mobilitypulse.api;

import dev.pseonkyaw.mobilitypulse.domain.PingEvent;
import dev.pseonkyaw.mobilitypulse.ingest.PingIngestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/pings")
@RequiredArgsConstructor
public class PingIngestController {

    private final PingIngestService ingest;

    @PostMapping
    public ResponseEntity<IngestResponse> accept(@Valid @RequestBody PingIngestRequest req) {
        PingEvent event = toEvent(req);
        int written = ingest.ingest(List.of(event));
        return ResponseEntity.accepted().body(new IngestResponse(written, written == 1 ? "accepted" : "rejected"));
    }

    @PostMapping("/batch")
    public ResponseEntity<IngestResponse> acceptBatch(@Valid @RequestBody List<PingIngestRequest> reqs) {
        List<PingEvent> batch = reqs.stream().map(PingIngestController::toEvent).toList();
        int written = ingest.ingest(batch);
        return ResponseEntity.accepted().body(new IngestResponse(written, "accepted"));
    }

    private static PingEvent toEvent(PingIngestRequest r) {
        return new PingEvent(
                r.deviceId(),
                r.mode(),
                r.lat(),
                r.lon(),
                r.speedKmh(),
                r.headingDeg(),
                r.ts() != null ? r.ts() : Instant.now()
        );
    }

    public record IngestResponse(int written, String status) {}
}
