package dev.pseonkyaw.mobilitypulse.api;

import dev.pseonkyaw.mobilitypulse.domain.TransportMode;

import java.time.Instant;

/**
 * Projection row returned from the {@code hotspot_1min} continuous aggregate.
 * {@code h3Cell} is serialized as a hex string so JS clients can pass it back
 * to Uber's h3-js without 64-bit integer precision loss.
 */
public record HotspotCell(
        Instant bucket,
        String h3Cell,
        TransportMode mode,
        long pingCount,
        Double avgSpeedKmh
) {}
