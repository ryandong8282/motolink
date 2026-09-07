package com.motolink.api.ride;

import com.motolink.api.ride.RideService.AddPointRequest;
import com.motolink.api.ride.RideService.RideResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rides")
public class RideController {
    private final RideService rides;

    public RideController(RideService rides) {
        this.rides = rides;
    }

    @PostMapping
    public RideResponse start(@RequestHeader("X-User-Id") UUID userId) {
        return rides.start(userId);
    }

    @PostMapping("/{rideId}/points")
    public RideResponse append(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID rideId,
            @Valid @RequestBody AddPointRequest request) {
        return rides.append(rideId, userId, request);
    }

    @PostMapping("/{rideId}/finish")
    public RideResponse finish(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID rideId) {
        return rides.finish(rideId, userId);
    }
}
