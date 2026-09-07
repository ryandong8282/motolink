package com.motolink.api.rtc;

import com.motolink.api.auth.AuthInterceptor;
import com.motolink.api.config.MotoLinkProperties;
import com.motolink.api.team.TeamService;
import java.time.Clock;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teams/{teamId}/rtc-ticket")
public class RtcTicketController {

    private final TeamService teamService;
    private final MotoLinkProperties properties;
    private final Clock clock;

    public RtcTicketController(
            TeamService teamService,
            MotoLinkProperties properties,
            Clock clock) {
        this.teamService = teamService;
        this.properties = properties;
        this.clock = clock;
    }

    @GetMapping
    RtcTicket getTicket(
            @PathVariable String teamId,
            @RequestAttribute(AuthInterceptor.USER_ID_ATTRIBUTE) String userId) {
        teamService.requireMember(teamId, userId);
        return new RtcTicket(
                properties.rtcProvider(),
                false,
                teamId,
                userId,
                null,
                null,
                Instant.now(clock).plusSeconds(300),
                "MVP 当前使用 Mock RTC；真实 UserSig 必须由服务端生成");
    }

    public record RtcTicket(
            String provider,
            boolean configured,
            String roomId,
            String userId,
            Long sdkAppId,
            String userSig,
            Instant expiresAt,
            String message) {
    }
}
