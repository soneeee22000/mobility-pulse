package dev.pseonkyaw.mobilitypulse.ingest;

import dev.pseonkyaw.mobilitypulse.domain.PingEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Orchestrates write-time ingestion: instruments latency, counts pings, and
 * delegates durability to {@link PingWriter}. All metrics are exposed under
 * the {@code mobility.ingest.*} namespace for Prometheus / Grafana.
 */
@Slf4j
@Service
public class PingIngestService {

    private final PingWriter writer;
    private final Counter pingsAccepted;
    private final Counter pingsRejected;
    private final Timer ingestLatency;

    public PingIngestService(PingWriter writer, MeterRegistry registry) {
        this.writer = writer;
        this.pingsAccepted = Counter.builder("mobility.ingest.pings")
                .tag("outcome", "accepted")
                .description("Number of pings successfully written")
                .register(registry);
        this.pingsRejected = Counter.builder("mobility.ingest.pings")
                .tag("outcome", "rejected")
                .description("Number of pings dropped due to validation or DB error")
                .register(registry);
        this.ingestLatency = Timer.builder("mobility.ingest.latency")
                .description("End-to-end ingest latency per batch")
                .publishPercentileHistogram()
                .register(registry);
    }

    public int ingest(List<PingEvent> batch) {
        if (batch.isEmpty()) {
            return 0;
        }
        long t0 = System.nanoTime();
        try {
            int written = writer.writeBatch(batch);
            pingsAccepted.increment(written);
            return written;
        } catch (RuntimeException ex) {
            pingsRejected.increment(batch.size());
            log.warn("Rejected batch of {} pings: {}", batch.size(), ex.getMessage());
            throw ex;
        } finally {
            ingestLatency.record(System.nanoTime() - t0, TimeUnit.NANOSECONDS);
        }
    }
}
