package com.motolink.api.rtc;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class TrtcUserSigService {

    private static final Pattern USER_ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,32}");

    public String generate(
            long sdkAppId,
            String userId,
            String secretKey,
            Duration ttl,
            Instant issuedAt) {
        validate(sdkAppId, userId, secretKey, ttl, issuedAt);

        long currentTime = issuedAt.getEpochSecond();
        long expireSeconds = ttl.toSeconds();
        String contentToSign = "TLS.identifier:" + userId + "\n"
                + "TLS.sdkappid:" + sdkAppId + "\n"
                + "TLS.time:" + currentTime + "\n"
                + "TLS.expire:" + expireSeconds + "\n";
        String signature = hmacSha256(contentToSign, secretKey);

        String document = "{"
                + "\"TLS.ver\":\"2.0\","
                + "\"TLS.identifier\":\"" + userId + "\","
                + "\"TLS.sdkappid\":" + sdkAppId + ","
                + "\"TLS.expire\":" + expireSeconds + ","
                + "\"TLS.time\":" + currentTime + ","
                + "\"TLS.sig\":\"" + signature + "\""
                + "}";

        return toTencentBase64Url(deflate(document.getBytes(UTF_8)));
    }

    private String hmacSha256(String content, String secretKey) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(secretKey.getBytes(UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(hmac.doFinal(content.getBytes(UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("无法生成 TRTC UserSig", exception);
        }
    }

    private byte[] deflate(byte[] input) {
        Deflater compressor = new Deflater();
        try {
            compressor.setInput(input);
            compressor.finish();
            ByteArrayOutputStream output = new ByteArrayOutputStream(input.length);
            byte[] buffer = new byte[512];
            while (!compressor.finished()) {
                int count = compressor.deflate(buffer);
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        } finally {
            compressor.end();
        }
    }

    private String toTencentBase64Url(byte[] input) {
        return Base64.getEncoder().encodeToString(input)
                .replace('+', '*')
                .replace('/', '-')
                .replace('=', '_');
    }

    private void validate(
            long sdkAppId,
            String userId,
            String secretKey,
            Duration ttl,
            Instant issuedAt) {
        if (sdkAppId <= 0) {
            throw new IllegalArgumentException("TRTC SDKAppID 必须为正数");
        }
        if (userId == null || !USER_ID_PATTERN.matcher(userId).matches()) {
            throw new IllegalArgumentException("TRTC UserID 只能包含字母、数字、下划线和连字符，且不超过 32 字节");
        }
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalArgumentException("TRTC SecretKey 不能为空");
        }
        if (ttl == null || ttl.isZero() || ttl.isNegative() || ttl.toSeconds() < 60) {
            throw new IllegalArgumentException("TRTC UserSig 有效期不得短于 60 秒");
        }
        if (issuedAt == null) {
            throw new IllegalArgumentException("TRTC UserSig 签发时间不能为空");
        }
    }
}
