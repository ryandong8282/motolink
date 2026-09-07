package com.motolink.api.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.motolink.api.config.MotoLinkProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class FloorServiceTest {

    @Test
    void grantsOnlyOneSpeakerAtATimeAndExpiresLease() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-07T00:00:00Z"));
        MotoLinkProperties properties = new MotoLinkProperties(
                Duration.ofHours(1), Duration.ofSeconds(45), Duration.ofSeconds(60), "mock");
        FloorService service = new FloorService(clock, properties);

        FloorService.AcquireResult first = service.request("team_1", "u_1", "一号");
        FloorService.AcquireResult blocked = service.request("team_1", "u_2", "二号");

        assertThat(first.granted()).isTrue();
        assertThat(blocked.granted()).isFalse();
        assertThat(blocked.lease().userId()).isEqualTo("u_1");

        clock.advance(Duration.ofSeconds(46));
        FloorService.AcquireResult afterExpiry = service.request("team_1", "u_2", "二号");
        assertThat(afterExpiry.granted()).isTrue();
        assertThat(afterExpiry.lease().userId()).isEqualTo("u_2");
    }

    @Test
    void exposesExpiredLeaseForRealtimeNotification() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-07T00:00:00Z"));
        MotoLinkProperties properties = new MotoLinkProperties(
                Duration.ofHours(1), Duration.ofSeconds(45), Duration.ofSeconds(60), "mock");
        FloorService service = new FloorService(clock, properties);
        service.request("team_1", "u_1", "一号");

        clock.advance(Duration.ofSeconds(45));

        assertThat(service.expireStale())
                .singleElement()
                .satisfies(lease -> {
                    assertThat(lease.teamId()).isEqualTo("team_1");
                    assertThat(lease.userId()).isEqualTo("u_1");
                });
        assertThat(service.current("team_1")).isNull();
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> instant;

        private MutableClock(Instant initial) {
            this.instant = new AtomicReference<>(initial);
        }

        void advance(Duration duration) {
            instant.updateAndGet(value -> value.plus(duration));
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant.get();
        }
    }
}
