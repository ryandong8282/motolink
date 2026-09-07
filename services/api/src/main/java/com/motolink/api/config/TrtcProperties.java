package com.motolink.api.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "motolink.trtc")
public record TrtcProperties(
        Long sdkAppId,
        String secretKey,
        Duration userSigTtl) {

    public TrtcProperties {
        sdkAppId = sdkAppId == null ? 0L : sdkAppId;
        secretKey = secretKey == null ? "" : secretKey.trim();
        userSigTtl = userSigTtl == null ? Duration.ofHours(24) : userSigTtl;
    }

    public boolean configured() {
        return sdkAppId > 0 && !secretKey.isBlank() && !userSigTtl.isZero() && !userSigTtl.isNegative();
    }
}
