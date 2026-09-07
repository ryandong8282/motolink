package com.motolink.api.nearby;

import com.motolink.api.nearby.LocationService.LocationUpdate;
import com.motolink.api.nearby.LocationService.NearbyRider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.util.List;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/nearby")
@Validated
public class NearbyController {
    private final LocationService locations;

    public NearbyController(LocationService locations) {
        this.locations = locations;
    }

    @PostMapping("/location")
    public void update(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody LocationUpdate request) {
        locations.update(userId, request);
    }

    @GetMapping
    public List<NearbyRider> nearby(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam @DecimalMin("-90") @DecimalMax("90") double latitude,
            @RequestParam @DecimalMin("-180") @DecimalMax("180") double longitude,
            @RequestParam(defaultValue = "10") @DecimalMin("0.1") @DecimalMax("10") double radiusKm) {
        return locations.nearby(userId, latitude, longitude, radiusKm);
    }
}
