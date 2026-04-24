package dev.pseonkyaw.mobilitypulse.simulator;

import dev.pseonkyaw.mobilitypulse.api.BoundingBox;
import dev.pseonkyaw.mobilitypulse.domain.PingEvent;
import dev.pseonkyaw.mobilitypulse.domain.TransportMode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Drives the Paris-scale synthetic workload. Activates when either the
 * {@code simulator} Spring profile is set OR {@code mobility.simulator.enabled}
 * is true. Produces {@code pingsPerSecond} ping events to the configured
 * Kafka topic so the normal ingest consumer writes them to TimescaleDB.
 */
@Slf4j
@Component
@Profile("!test")
@ConditionalOnProperty(prefix = "mobility.simulator", name = "enabled", havingValue = "true")
public class MobilitySimulator {

    private static final long TICK_MS = 1000L;

    private final SimulatorProperties props;
    private final KafkaTemplate<String, Object> kafka;
    private final String topic;
    private final List<SyntheticDevice> fleet = new ArrayList<>();

    public MobilitySimulator(
            SimulatorProperties props,
            KafkaTemplate<String, Object> kafka,
            @Value("${mobility.ingest.topic:pings.raw}") String topic
    ) {
        this.props = props;
        this.kafka = kafka;
        this.topic = topic;
    }

    @PostConstruct
    void seedFleet() {
        BoundingBox bbox = props.boundingBox();
        TransportMode[] modes = TransportMode.values();
        for (int i = 0; i < props.devices(); i++) {
            TransportMode mode = modes[i % modes.length];
            fleet.add(new SyntheticDevice("dev-" + UUID.randomUUID(), mode, bbox));
        }
        log.info("Simulator seeded with {} devices in bbox {}", fleet.size(), bbox);
    }

    @Scheduled(fixedRate = TICK_MS)
    public void tick() {
        int target = props.pingsPerSecond();
        BoundingBox bbox = props.boundingBox();
        double tickSeconds = TICK_MS / 1000.0;

        for (int i = 0; i < target && !fleet.isEmpty(); i++) {
            SyntheticDevice device = fleet.get(ThreadLocalRandom.current().nextInt(fleet.size()));
            PingEvent event = device.advance(tickSeconds, bbox);
            kafka.send(topic, event.deviceId(), event);
        }
        if (log.isDebugEnabled()) {
            log.debug("Simulator published ~{} pings this tick", target);
        }
    }
}
