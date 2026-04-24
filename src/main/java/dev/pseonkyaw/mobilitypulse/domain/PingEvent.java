package dev.pseonkyaw.mobilitypulse.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Wire-format representation of a single device ping. Produced by upstream
 * sensors / mobile apps / simulators, consumed from Kafka topic
 * {@code pings.raw} or POSTed to {@code /api/v1/pings}.
 */
public record PingEvent(
        String deviceId,
        TransportMode mode,
        double lat,
        double lon,
        Double speedKmh,
        Integer headingDeg,
        Instant ts
) {

    @JsonCreator
    public PingEvent(
            @JsonProperty("deviceId") String deviceId,
            @JsonProperty("mode") TransportMode mode,
            @JsonProperty("lat") double lat,
            @JsonProperty("lon") double lon,
            @JsonProperty("speedKmh") Double speedKmh,
            @JsonProperty("headingDeg") Integer headingDeg,
            @JsonProperty("ts") Instant ts
    ) {
        this.deviceId = deviceId;
        this.mode = mode;
        this.lat = lat;
        this.lon = lon;
        this.speedKmh = speedKmh;
        this.headingDeg = headingDeg;
        this.ts = ts;
    }
}
