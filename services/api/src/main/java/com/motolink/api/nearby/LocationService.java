package com.motolink.api.nearby;

import com.motolink.api.user.AppUser;
import com.motolink.api.user.AppUserRepository;
import com.motolink.api.user.UserService;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocationService {
    private static final String GEO_KEY = "motolink:nearby:geo";
    private static final Duration PRESENCE_TTL = Duration.ofSeconds(45);

    private final StringRedisTemplate redis;
    private final AppUserRepository users;
    private final UserService userService;

    public LocationService(
            StringRedisTemplate redis,
            AppUserRepository users,
            UserService userService) {
        this.redis = redis;
        this.users = users;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public void update(UUID userId, LocationUpdate request) {
        userService.require(userId);
        redis.opsForGeo().add(GEO_KEY, new Point(request.longitude(), request.latitude()), userId.toString());
        redis.opsForValue().set(presenceKey(userId), "1", PRESENCE_TTL);
    }

    @Transactional(readOnly = true)
    public List<NearbyRider> nearby(UUID currentUserId, double latitude, double longitude, double radiusKm) {
        userService.require(currentUserId);
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                .newGeoRadiusArgs()
                .includeDistance()
                .includeCoordinates()
                .sortAscending()
                .limit(50);

        GeoResults<RedisGeoCommands.GeoLocation<String>> results = redis.opsForGeo().radius(
                GEO_KEY,
                new Circle(new Point(longitude, latitude), new Distance(radiusKm, Metrics.KILOMETERS)),
                args);

        if (results == null) return List.of();

        List<NearbyRider> riders = new ArrayList<>();
        for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : results) {
            String member = result.getContent().getName();
            if (member.equals(currentUserId.toString())) continue;
            UUID userId;
            try {
                userId = UUID.fromString(member);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            if (Boolean.FALSE.equals(redis.hasKey(presenceKey(userId)))) continue;
            AppUser user = users.findById(userId).orElse(null);
            Point point = result.getContent().getPoint();
            if (user == null || point == null) continue;

            riders.add(new NearbyRider(
                    user.getId(),
                    user.getNickname(),
                    user.getMotorcycle(),
                    result.getDistance() == null ? 0 : result.getDistance().getValue() * 1000,
                    roundForStranger(point.getY()),
                    roundForStranger(point.getX()),
                    true));
        }
        return riders;
    }

    private static String presenceKey(UUID userId) {
        return "motolink:nearby:presence:" + userId;
    }

    private static double roundForStranger(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    public static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6_371_000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public record LocationUpdate(
            @DecimalMin("-90") @DecimalMax("90") double latitude,
            @DecimalMin("-180") @DecimalMax("180") double longitude,
            Double speedKmh,
            Double heading) {}

    public record NearbyRider(
            UUID userId,
            String nickname,
            String motorcycle,
            double distanceMeters,
            double latitude,
            double longitude,
            boolean online) {}
}
