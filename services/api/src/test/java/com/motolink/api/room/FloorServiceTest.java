package com.motolink.api.room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class FloorServiceTest {
    @Test
    @SuppressWarnings("unchecked")
    void grantsFloorWhenRedisSetNxSucceeds() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        UUID roomId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), eq(userId.toString()), eq(Duration.ofSeconds(15))))
                .thenReturn(true);

        FloorService.FloorResponse response = new FloorService(redis).request(roomId, userId);

        assertThat(response.granted()).isTrue();
        assertThat(response.speakerId()).isEqualTo(userId);
        assertThat(response.leaseMillis()).isEqualTo(15_000);
    }
}
