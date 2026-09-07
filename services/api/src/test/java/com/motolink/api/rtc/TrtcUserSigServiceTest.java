package com.motolink.api.rtc;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.zip.Inflater;
import org.junit.jupiter.api.Test;

class TrtcUserSigServiceTest {

    private final TrtcUserSigService service = new TrtcUserSigService();

    @Test
    void generatesDeterministicTencentUserSigDocument() {
        Instant issuedAt = Instant.ofEpochSecond(1_700_000_000L);

        String first = service.generate(
                1_400_000_001L,
                "u_rider-01",
                "test-secret-key",
                Duration.ofHours(24),
                issuedAt);
        String second = service.generate(
                1_400_000_001L,
                "u_rider-01",
                "test-secret-key",
                Duration.ofHours(24),
                issuedAt);

        assertThat(first).isEqualTo(second);
        assertThat(first).doesNotContain("test-secret-key");

        String document = inflate(first);
        assertThat(document)
                .contains("\"TLS.ver\":\"2.0\"")
                .contains("\"TLS.identifier\":\"u_rider-01\"")
                .contains("\"TLS.sdkappid\":1400000001")
                .contains("\"TLS.expire\":86400")
                .contains("\"TLS.time\":1700000000")
                .contains("\"TLS.sig\":");
    }

    @Test
    void rejectsUserIdOutsideTencentConstraints() {
        assertThatThrownBy(() -> service.generate(
                1_400_000_001L,
                "包含中文",
                "test-secret-key",
                Duration.ofHours(24),
                Instant.EPOCH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UserID");
    }

    private String inflate(String userSig) {
        String standardBase64 = userSig
                .replace('*', '+')
                .replace('-', '/')
                .replace('_', '=');
        byte[] compressed = Base64.getDecoder().decode(standardBase64);

        Inflater inflater = new Inflater();
        try {
            inflater.setInput(compressed);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[512];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                if (count == 0 && inflater.needsInput()) {
                    break;
                }
                output.write(buffer, 0, count);
            }
            return output.toString(UTF_8);
        } catch (Exception exception) {
            throw new AssertionError("Unable to decode generated UserSig", exception);
        } finally {
            inflater.end();
        }
    }
}
