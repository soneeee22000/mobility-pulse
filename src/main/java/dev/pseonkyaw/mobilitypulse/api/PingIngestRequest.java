package dev.pseonkyaw.mobilitypulse.api;

import dev.pseonkyaw.mobilitypulse.domain.TransportMode;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record PingIngestRequest(
        @NotBlank String deviceId,
        @NotNull  TransportMode mode,

        @DecimalMin(value = "-90.0",  inclusive = true)
        @DecimalMax(value =  "90.0",  inclusive = true)
        double lat,

        @DecimalMin(value = "-180.0", inclusive = true)
        @DecimalMax(value =  "180.0", inclusive = true)
        double lon,

        @DecimalMin(value = "0.0") @DecimalMax(value = "400.0")
        Double speedKmh,

        @Min(0) @Max(359) Integer headingDeg,

        Instant ts
) {}
