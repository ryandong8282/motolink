package com.motolink.api.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "motolink")
public record MotoLinkProperties(
        Duration sessionTtl,
        Duration floorLease,
        Duration locationOnlineWindow,
        String rtcProvider) {

    public MotoLinkProperties {
        sessionTtl = sessionTtl == null ? Duration.ofHours(24) : sessionTtl;
        floorLease = floorLease == null ? Duration.ofSeconds(45) : floorLease;
        locationOnlineWindow = locationOnlineWindow == null ? Duration.ofSeconds(60) : locationOnlineWindow;
        rtcProvider = rtcProvider == null || rtcProvider.isBlank() ? "mock" : rtcProvider;
    }
}
