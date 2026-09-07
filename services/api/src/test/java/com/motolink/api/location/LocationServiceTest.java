package com.motolink.api.location;

import static org.assertj.core.api.Assertions.assertThat;

import com.motolink.api.config.MotoLinkProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class LocationServiceTest {

    @Test
    void returnsNearbyUsersAndFuzzesPublicCoordinates() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-07T00:00:00Z"), ZoneOffset.UTC);
        MotoLinkProperties properties = new MotoLinkProperties(
                Duration.ofHours(1), Duration.ofSeconds(45), Duration.ofSeconds(60), "mock");
        LocationService service = new LocationService(clock, properties);
        service.update(
                "u_2",
                "附近车友",
                new LocationService.LocationUpdate(39.904234, 116.407456, 10.0, 0.0, 0.0),
                null);

        List<LocationService.NearbyUser> result = service.nearby(
                "u_1", 39.9042, 116.4074, 1_000);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().latitude()).isEqualTo(39.904);
        assertThat(result.getFirst().longitude()).isEqualTo(116.407);
        assertThat(result.getFirst().distanceMeters()).isLessThan(20);
    }
}
