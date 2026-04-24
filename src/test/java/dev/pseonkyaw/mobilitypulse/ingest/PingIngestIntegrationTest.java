package dev.pseonkyaw.mobilitypulse.ingest;

import dev.pseonkyaw.mobilitypulse.TestcontainersConfiguration;
import dev.pseonkyaw.mobilitypulse.api.BoundingBox;
import dev.pseonkyaw.mobilitypulse.api.HotspotCell;
import dev.pseonkyaw.mobilitypulse.domain.PingEvent;
import dev.pseonkyaw.mobilitypulse.domain.TransportMode;
import dev.pseonkyaw.mobilitypulse.query.HotspotService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the full stack against a live {@code timescale/timescaledb-ha:pg17}
 * container, runs Flyway, writes a deterministic set of pings via
 * {@link PingIngestService}, and asserts the cagg read path returns the
 * expected rollup. Exercises: PostGIS geography column, Uber H3 indexing,
 * TimescaleDB continuous aggregate with {@code materialized_only=false}.
 */
@SpringBootTest(
        classes = {
                dev.pseonkyaw.mobilitypulse.MobilityPulseApplication.class
        },
        properties = {
                "mobility.simulator.enabled=false",
                "spring.autoconfigure.exclude=" + "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
        }
)
@Import(TestcontainersConfiguration.class)
class PingIngestIntegrationTest {

    @Autowired
    private PingIngestService ingestService;

    @Autowired
    private HotspotService hotspotService;

    @Test
    void pingsLandInHotspotsCagg() {
        Instant now = Instant.now();

        List<PingEvent> batch = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            batch.add(new PingEvent(
                    "test-dev-" + i,
                    TransportMode.BIKE,
                    48.8566 + (i % 10) * 0.0001,
                    2.3522 + (i % 10) * 0.0001,
                    20.0 + i,
                    90,
                    now.minusSeconds(i)
            ));
        }

        int written = ingestService.ingest(batch);
        assertThat(written).isEqualTo(50);

        BoundingBox paris = BoundingBox.paris();
        List<HotspotCell> cells = hotspotService.hotspots(
                now.minus(Duration.ofMinutes(5)),
                now.plus(Duration.ofMinutes(1)),
                paris,
                TransportMode.BIKE,
                500
        );

        long totalPings = cells.stream().mapToLong(HotspotCell::pingCount).sum();
        assertThat(totalPings).isGreaterThanOrEqualTo(50);
        assertThat(cells).allSatisfy(c -> {
            assertThat(c.mode()).isEqualTo(TransportMode.BIKE);
            assertThat(c.h3Cell()).hasSize(15);
        });
    }
}
