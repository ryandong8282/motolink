package com.motolink.api.ptt;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.motolink.api.mvp.MvpRepository;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teams/{roomId}/ptt")
public class PttController {

    private final FloorControlService floorControl;
    private final MvpRepository repository;

    public PttController(FloorControlService floorControl, MvpRepository repository) {
        this.floorControl = floorControl;
        this.repository = repository;
    }

    @PostMapping("/request")
    public FloorControlService.LeaseResult request(
        @PathVariable UUID roomId,
        @Valid @RequestBody FloorRequest request
    ) {
        repository.requireMembership(roomId, request.userId());
        return floorControl.request(roomId, request.userId());
    }

    @PostMapping("/heartbeat")
    public FloorControlService.LeaseResult heartbeat(
        @PathVariable UUID roomId,
        @Valid @RequestBody FloorRequest request
    ) {
        repository.requireMembership(roomId, request.userId());
        return floorControl.heartbeat(roomId, request.userId());
    }

    @PostMapping("/release")
    public FloorControlService.LeaseResult release(
        @PathVariable UUID roomId,
        @Valid @RequestBody FloorRequest request
    ) {
        repository.requireMembership(roomId, request.userId());
        return floorControl.release(roomId, request.userId());
    }

    @GetMapping
    public FloorControlService.LeaseResult current(
        @PathVariable UUID roomId,
        @RequestParam UUID requesterId
    ) {
        repository.requireMembership(roomId, requesterId);
        return floorControl.current(roomId);
    }

    public record FloorRequest(@NotNull UUID userId) {
    }
}
