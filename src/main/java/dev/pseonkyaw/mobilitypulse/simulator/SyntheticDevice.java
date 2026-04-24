package dev.pseonkyaw.mobilitypulse.simulator;

import dev.pseonkyaw.mobilitypulse.api.BoundingBox;
import dev.pseonkyaw.mobilitypulse.domain.PingEvent;
import dev.pseonkyaw.mobilitypulse.domain.TransportMode;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A single synthetic mobility agent. Moves each tick along a heading at a
 * mode-appropriate speed, randomly turns, and wraps back into the bounding
 * box if it strays. Not thread-safe — each device is advanced by a single
 * scheduled thread.
 */
final class SyntheticDevice {

    private static final double METERS_PER_DEG_LAT = 111_320.0;

    private final String deviceId;
    private final TransportMode mode;
    private double lat;
    private double lon;
    private double speedKmh;
    private int headingDeg;

    SyntheticDevice(String deviceId, TransportMode mode, BoundingBox bbox) {
        this.deviceId = deviceId;
        this.mode = mode;
        this.lat = randomBetween(bbox.south(), bbox.north());
        this.lon = randomBetween(bbox.west(), bbox.east());
        this.speedKmh = baseSpeedFor(mode);
        this.headingDeg = ThreadLocalRandom.current().nextInt(360);
    }

    PingEvent advance(double tickSeconds, BoundingBox bbox) {
        if (ThreadLocalRandom.current().nextDouble() < 0.1) {
            headingDeg = (headingDeg + ThreadLocalRandom.current().nextInt(-45, 46) + 360) % 360;
            speedKmh = Math.max(0.0, baseSpeedFor(mode) + ThreadLocalRandom.current().nextDouble(-5.0, 5.0));
        }

        double distanceMeters = speedKmh * 1000.0 / 3600.0 * tickSeconds;
        double headingRad = Math.toRadians(headingDeg);
        double dLat = Math.cos(headingRad) * distanceMeters / METERS_PER_DEG_LAT;
        double dLon = Math.sin(headingRad) * distanceMeters
                / (METERS_PER_DEG_LAT * Math.cos(Math.toRadians(lat)));

        lat += dLat;
        lon += dLon;

        if (lat < bbox.south() || lat > bbox.north() || lon < bbox.west() || lon > bbox.east()) {
            lat = randomBetween(bbox.south(), bbox.north());
            lon = randomBetween(bbox.west(), bbox.east());
            headingDeg = ThreadLocalRandom.current().nextInt(360);
        }

        return new PingEvent(deviceId, mode, lat, lon, speedKmh, headingDeg, Instant.now());
    }

    private static double baseSpeedFor(TransportMode mode) {
        return switch (mode) {
            case PEDESTRIAN -> 4.5;
            case BIKE       -> 18.0;
            case SCOOTER    -> 22.0;
            case CAR        -> 35.0;
            case BUS        -> 25.0;
            case TRAM       -> 28.0;
        };
    }

    private static double randomBetween(double lo, double hi) {
        return lo + ThreadLocalRandom.current().nextDouble() * (hi - lo);
    }
}
