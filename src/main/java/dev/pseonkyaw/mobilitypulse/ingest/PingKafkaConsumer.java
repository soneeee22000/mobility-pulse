package dev.pseonkyaw.mobilitypulse.ingest;

import dev.pseonkyaw.mobilitypulse.domain.PingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Consumes PingEvents from Kafka in batches and flushes them to TimescaleDB.
 * Batch mode is used because the hypertable insert path is ~5x faster with
 * multi-row INSERTs than with single-row round trips.
 * <p>
 * Offsets are manually acknowledged after a successful DB write — a poison
 * pill is isolated by {@code ErrorHandlingDeserializer} at the converter
 * layer, so only genuine DB failures cause a rewind.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PingKafkaConsumer {

    private final PingIngestService ingest;

    @KafkaListener(
            topics = "${mobility.ingest.topic:pings.raw}",
            groupId = "${spring.kafka.consumer.group-id:mobility-pulse}",
            batch = "true",
            containerFactory = "batchKafkaListenerContainerFactory"
    )
    public void onBatch(List<PingEvent> batch, Acknowledgment ack) {
        try {
            int written = ingest.ingest(batch);
            if (log.isDebugEnabled()) {
                log.debug("Consumed {} pings from Kafka, wrote {}", batch.size(), written);
            }
            ack.acknowledge();
        } catch (RuntimeException ex) {
            log.error("Batch ingest failed; offsets NOT acknowledged", ex);
            throw ex;
        }
    }
}
