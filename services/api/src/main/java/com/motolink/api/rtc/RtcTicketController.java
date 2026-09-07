package com.motolink.api.rtc;

import com.motolink.api.auth.AuthInterceptor;
import com.motolink.api.config.MotoLinkProperties;
import com.motolink.api.config.TrtcProperties;
import com.motolink.api.team.TeamService;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
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
    private final TrtcProperties trtcProperties;
    private final TrtcUserSigService userSigService;
    private final Clock clock;

    public RtcTicketController(
            TeamService teamService,
            MotoLinkProperties properties,
            TrtcProperties trtcProperties,
            TrtcUserSigService userSigService,
            Clock clock) {
        this.teamService = teamService;
        this.properties = properties;
        this.trtcProperties = trtcProperties;
        this.userSigService = userSigService;
        this.clock = clock;
    }

    @GetMapping
    RtcTicket getTicket(
            @PathVariable String teamId,
            @RequestAttribute(AuthInterceptor.USER_ID_ATTRIBUTE) String userId) {
        teamService.requireMember(teamId, userId);

        String provider = properties.rtcProvider().trim().toLowerCase(Locale.ROOT);
        Instant issuedAt = Instant.now(clock);
        if (!"trtc".equals(provider)) {
            return new RtcTicket(
                    provider,
                    false,
                    teamId,
                    userId,
                    null,
                    null,
                    issuedAt.plusSeconds(300),
                    "当前 RTC Provider 不是 trtc，请设置 RTC_PROVIDER=trtc");
        }
        if (!trtcProperties.configured()) {
            return new RtcTicket(
                    "trtc",
                    false,
                    teamId,
                    userId,
                    trtcProperties.sdkAppId() > 0 ? trtcProperties.sdkAppId() : null,
                    null,
                    issuedAt.plusSeconds(300),
                    "TRTC 尚未配置，请在服务端设置 TRTC_SDK_APP_ID 和 TRTC_SECRET_KEY");
        }

        String userSig = userSigService.generate(
                trtcProperties.sdkAppId(),
                userId,
                trtcProperties.secretKey(),
                trtcProperties.userSigTtl(),
                issuedAt);
        return new RtcTicket(
                "trtc",
                true,
                teamId,
                userId,
                trtcProperties.sdkAppId(),
                userSig,
                issuedAt.plus(trtcProperties.userSigTtl()),
                "腾讯 TRTC 纯音频票据已签发");
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
