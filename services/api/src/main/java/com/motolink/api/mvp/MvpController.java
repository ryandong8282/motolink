package com.motolink.api.mvp;

import static com.motolink.api.mvp.MvpModels.AdminOverview;
import static com.motolink.api.mvp.MvpModels.LocationView;
import static com.motolink.api.mvp.MvpModels.RideView;
import static com.motolink.api.mvp.MvpModels.TeamView;
import static com.motolink.api.mvp.MvpModels.UserView;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class MvpController {

    private final MvpRepository repository;
    private final String rtcProvider;

    public MvpController(
        MvpRepository repository,
        @Value("${motolink.rtc.provider}") String rtcProvider
    ) {
        this.repository = repository;
        this.rtcProvider = rtcProvider;
    }

    @PostMapping("/auth/dev-login")
    public AuthResponse devLogin(@Valid @RequestBody DevLoginRequest request) {
        UserView user = repository.upsertUser(request.phone(), request.nickname().trim());
        return new AuthResponse(user, "dev:" + user.id());
    }

    @PostMapping("/teams")
    @ResponseStatus(HttpStatus.CREATED)
    public TeamView createTeam(@Valid @RequestBody CreateTeamRequest request) {
        return repository.createTeam(request.leaderId(), request.name().trim(), request.maxMembers());
    }

    @PostMapping("/teams/join")
    public TeamView joinTeam(@Valid @RequestBody JoinTeamRequest request) {
        return repository.joinTeam(request.userId(), request.roomCode());
    }

    @GetMapping("/teams/{roomId}")
    public TeamView getTeam(@PathVariable UUID roomId) {
        return repository.getTeam(roomId);
    }

    @PostMapping("/teams/{roomId}/locations")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateLocation(
        @PathVariable UUID roomId,
        @Valid @RequestBody LocationUpdateRequest request
    ) {
        repository.upsertLocation(
            roomId,
            request.userId(),
            request.latitude(),
            request.longitude(),
            request.speedMps(),
            request.heading(),
            request.recordedAt()
        );
    }

    @GetMapping("/teams/{roomId}/locations")
    public List<LocationView> listLocations(
        @PathVariable UUID roomId,
        @RequestParam UUID requesterId
    ) {
        return repository.listLocations(roomId, requesterId);
    }

    @PostMapping("/rides/start")
    @ResponseStatus(HttpStatus.CREATED)
    public RideView startRide(@Valid @RequestBody StartRideRequest request) {
        return repository.startRide(request.userId(), request.roomId());
    }

    @PostMapping("/rides/{rideId}/points")
    public AppendPointsResponse appendTrackPoints(
        @PathVariable UUID rideId,
        @Valid @RequestBody AppendTrackPointsRequest request
    ) {
        List<MvpRepository.TrackPointInput> points = request.points().stream()
            .map(point -> new MvpRepository.TrackPointInput(
                point.sequenceNo(),
                point.latitude(),
                point.longitude(),
                point.speedMps(),
                point.recordedAt()
            ))
            .toList();
        return new AppendPointsResponse(repository.appendTrackPoints(rideId, points));
    }

    @PostMapping("/rides/{rideId}/finish")
    public RideView finishRide(
        @PathVariable UUID rideId,
        @Valid @RequestBody FinishRideRequest request
    ) {
        return repository.finishRide(rideId, request.distanceMeters(), request.maxSpeedMps());
    }

    @PostMapping("/rtc/token")
    public RtcTokenResponse rtcToken(@Valid @RequestBody RtcTokenRequest request) {
        repository.requireMembership(request.roomId(), request.userId());
        // Deliberately non-secret. Replace this adapter with a vendor-side token signer.
        return new RtcTokenResponse(
            rtcProvider,
            "mock".equalsIgnoreCase(rtcProvider) ? "local-dev-token" : "not-configured",
            OffsetDateTime.now().plusMinutes(15)
        );
    }

    @GetMapping("/admin/overview")
    public AdminOverview adminOverview() {
        return repository.overview();
    }

    public record DevLoginRequest(
        @NotBlank
        @Pattern(regexp = "^[0-9+ -]{6,20}$", message = "must look like a phone number")
        String phone,
        @NotBlank @Size(max = 64) String nickname
    ) {
    }

    public record AuthResponse(UserView user, String token) {
    }

    public record CreateTeamRequest(
        @NotNull UUID leaderId,
        @NotBlank @Size(max = 80) String name,
        @Min(2) @Max(100) int maxMembers
    ) {
    }

    public record JoinTeamRequest(
        @NotNull UUID userId,
        @NotBlank @Pattern(regexp = "^[0-9]{6}$") String roomCode
    ) {
    }

    public record LocationUpdateRequest(
        @NotNull UUID userId,
        @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") double longitude,
        @DecimalMin("0.0") Double speedMps,
        @DecimalMin("0.0") @DecimalMax("360.0") Double heading,
        @NotNull OffsetDateTime recordedAt
    ) {
    }

    public record StartRideRequest(
        @NotNull UUID userId,
        UUID roomId
    ) {
    }

    public record AppendTrackPointsRequest(
        @NotEmpty @Size(max = 200) List<@Valid TrackPointRequest> points
    ) {
    }

    public record TrackPointRequest(
        @Min(0) long sequenceNo,
        @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") double longitude,
        @DecimalMin("0.0") Double speedMps,
        @NotNull OffsetDateTime recordedAt
    ) {
    }

    public record AppendPointsResponse(int accepted) {
    }

    public record FinishRideRequest(
        @DecimalMin("0.0") double distanceMeters,
        @DecimalMin("0.0") double maxSpeedMps
    ) {
    }

    public record RtcTokenRequest(
        @NotNull UUID roomId,
        @NotNull UUID userId
    ) {
    }

    public record RtcTokenResponse(
        String provider,
        String token,
        OffsetDateTime expiresAt
    ) {
    }
}
