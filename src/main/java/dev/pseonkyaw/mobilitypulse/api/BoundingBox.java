package dev.pseonkyaw.mobilitypulse.api;

/**
 * Geographic bounding box in WGS84 decimal degrees.
 * Used to scope hotspot queries to a viewport or city boundary.
 */
public record BoundingBox(double south, double west, double north, double east) {

    public BoundingBox {
        if (south > north) {
            throw new IllegalArgumentException("south > north: " + south + " > " + north);
        }
        if (west > east) {
            throw new IllegalArgumentException("west > east: " + west + " > " + east);
        }
    }

    public static BoundingBox paris() {
        return new BoundingBox(48.815, 2.224, 48.902, 2.470);
    }
}
