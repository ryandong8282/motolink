package com.motolink.api.team;

import com.motolink.api.auth.AuthInterceptor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    TeamService.TeamView create(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTRIBUTE) String userId,
            @RequestAttribute(AuthInterceptor.NICKNAME_ATTRIBUTE) String nickname,
            @Valid @RequestBody CreateTeamRequest request) {
        return teamService.create(
                userId,
                nickname,
                request.name(),
                request.routeNote(),
                request.maxMembers(),
                request.publicRoom());
    }

    @PostMapping("/join")
    TeamService.TeamView join(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTRIBUTE) String userId,
            @RequestAttribute(AuthInterceptor.NICKNAME_ATTRIBUTE) String nickname,
            @Valid @RequestBody JoinTeamRequest request) {
        return teamService.joinByRoomCode(userId, nickname, request.roomCode());
    }

    @GetMapping
    List<TeamService.TeamView> list(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTRIBUTE) String userId) {
        return teamService.listForUser(userId);
    }

    @GetMapping("/{teamId}")
    TeamService.TeamView get(
            @PathVariable String teamId,
            @RequestAttribute(AuthInterceptor.USER_ID_ATTRIBUTE) String userId) {
        return teamService.getForMember(teamId, userId);
    }

    @PostMapping("/{teamId}/leave")
    TeamService.LeaveResult leave(
            @PathVariable String teamId,
            @RequestAttribute(AuthInterceptor.USER_ID_ATTRIBUTE) String userId) {
        return teamService.leave(teamId, userId);
    }

    public record CreateTeamRequest(
            @NotBlank(message = "车队名称不能为空")
            @Size(min = 2, max = 30, message = "车队名称长度应为 2-30 个字符")
            String name,
            @Size(max = 100, message = "路线备注最多 100 个字符")
            String routeNote,
            @Min(value = 2, message = "人数上限不能小于 2")
            @Max(value = 50, message = "MVP 人数上限不能超过 50")
            int maxMembers,
            boolean publicRoom) {
    }

    public record JoinTeamRequest(
            @NotBlank(message = "房间码不能为空")
            @Pattern(regexp = "\\d{6}", message = "房间码应为 6 位数字")
            String roomCode) {
    }
}
