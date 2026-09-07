package com.motolink.team;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping
    public List<TeamService.TeamView> list() {
        return teamService.list();
    }

    @GetMapping("/{teamId}")
    public TeamService.TeamView get(@PathVariable UUID teamId) {
        return teamService.get(teamId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeamService.TeamView create(@Valid @RequestBody CreateTeamRequest request) {
        return teamService.create(new TeamService.CreateTeam(
                request.name(),
                request.ownerId(),
                request.routeNote(),
                request.capacity(),
                request.privateRoom()));
    }

    @PostMapping("/join")
    public TeamService.TeamView join(@Valid @RequestBody JoinTeamRequest request) {
        return teamService.join(request.roomCode(), request.userId());
    }

    public record CreateTeamRequest(
            @NotBlank @Size(max = 80) String name,
            @NotNull UUID ownerId,
            @Size(max = 255) String routeNote,
            @Min(2) @Max(100) int capacity,
            boolean privateRoom) {
    }

    public record JoinTeamRequest(
            @NotBlank @Size(min = 6, max = 12) String roomCode,
            @NotNull UUID userId) {
    }
}
