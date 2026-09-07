package com.motolink.ptt;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teams/{teamId}/floor")
public class FloorController {

    private final FloorService floorService;

    public FloorController(FloorService floorService) {
        this.floorService = floorService;
    }

    @PostMapping("/claim")
    @ResponseStatus(HttpStatus.CREATED)
    public FloorService.FloorLease claim(
            @PathVariable UUID teamId,
            @Valid @RequestBody ClaimRequest request) {
        return floorService.claim(teamId, request.userId());
    }

    @PostMapping("/heartbeat")
    public FloorService.FloorLease heartbeat(
            @PathVariable UUID teamId,
            @Valid @RequestBody LeaseRequest request) {
        return floorService.heartbeat(teamId, request.userId(), request.leaseToken());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(
            @PathVariable UUID teamId,
            @Valid @RequestBody LeaseRequest request) {
        floorService.release(teamId, request.userId(), request.leaseToken());
    }

    public record ClaimRequest(@NotNull UUID userId) {
    }

    public record LeaseRequest(
            @NotNull UUID userId,
            @NotNull UUID leaseToken) {
    }
}
