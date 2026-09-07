package com.motolink.api.location;

import com.motolink.api.config.MotoLinkProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class LocationService {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private final Map<String, MemberLocation> locations = new ConcurrentHashMap<>();
    private final Clock clock;
    private final MotoLinkProperties properties;

    public LocationService(Clock clock, MotoLinkProperties properties) {
        this.clock = clock;
        this.properties = properties;
    }

    public MemberLocation update(
            String userId,
            String nickname,
            LocationUpdate update,
            String teamId) {
        MemberLocation location = new MemberLocation(
                userId,
                nickname,
                update.latitude(),
                update.longitude(),
                update.accuracy(),
                update.speed(),
                update.bearing(),
                teamId,
                Instant.now(clock));
        locations.put(userId, location);
        return location;
    }

    public List<NearbyUser> nearby(
            String currentUserId,
            double latitude,
            double longitude,
            int radiusMeters) {
        Instant cutoff = Instant.now(clock).minus(properties.locationOnlineWindow());
        return locations.values().stream()
                .filter(location -> !location.userId().equals(currentUserId))
                .filter(location -> !location.updatedAt().isBefore(cutoff))
                .map(location -> new NearbyUser(
                        location.userId(),
                        location.nickname(),
                        roundCoordinate(location.latitude()),
                        roundCoordinate(location.longitude()),
                        Math.round(distanceMeters(
                                latitude,
                                longitude,
                                location.latitude(),
                                location.longitude())),
                        location.updatedAt()))
                .filter(user -> user.distanceMeters() <= radiusMeters)
                .sorted(Comparator.comparingLong(NearbyUser::distanceMeters))
                .toList();
    }

    public List<MemberLocation> teamLocations(String teamId) {
        Instant cutoff = Instant.now(clock).minus(properties.locationOnlineWindow());
        return locations.values().stream()
                .filter(location -> teamId.equals(location.teamId()))
                .filter(location -> !location.updatedAt().isBefore(cutoff))
                .sorted(Comparator.comparing(MemberLocation::updatedAt).reversed())
                .toList();
    }

    static double distanceMeters(
            double latitude1,
            double longitude1,
            double latitude2,
            double longitude2) {
        double latitudeDistance = Math.toRadians(latitude2 - latitude1);
        double longitudeDistance = Math.toRadians(longitude2 - longitude1);
        double a = Math.sin(latitudeDistance / 2) * Math.sin(latitudeDistance / 2)
                + Math.cos(Math.toRadians(latitude1))
                * Math.cos(Math.toRadians(latitude2))
                * Math.sin(longitudeDistance / 2)
                * Math.sin(longitudeDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    private double roundCoordinate(double value) {
        return Math.round(value * 1_000.0) / 1_000.0;
    }

    public record LocationUpdate(
            double latitude,
            double longitude,
            Double accuracy,
            Double speed,
            Double bearing) {
    }

    public record MemberLocation(
            String userId,
            String nickname,
            double latitude,
            double longitude,
            Double accuracy,
            Double speed,
            Double bearing,
            String teamId,
            Instant updatedAt) {
    }

    public record NearbyUser(
            String userId,
            String nickname,
            double latitude,
            double longitude,
            long distanceMeters,
            Instant updatedAt) {
    }
}
