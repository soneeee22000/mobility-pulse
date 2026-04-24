package dev.pseonkyaw.mobilitypulse.simulator;

import dev.pseonkyaw.mobilitypulse.api.BoundingBox;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mobility.simulator")
public record SimulatorProperties(
        boolean enabled,
        int devices,
        int pingsPerSecond,
        Bbox bbox
) {
    public SimulatorProperties {
        if (devices <= 0) devices = 1000;
        if (pingsPerSecond <= 0) pingsPerSecond = 200;
    }

    public BoundingBox boundingBox() {
        return new BoundingBox(bbox.south, bbox.west, bbox.north, bbox.east);
    }

    public record Bbox(double south, double west, double north, double east) {}
}
