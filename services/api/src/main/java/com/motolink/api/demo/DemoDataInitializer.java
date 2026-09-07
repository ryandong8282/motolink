package com.motolink.api.demo;

import com.motolink.api.nearby.LocationService;
import com.motolink.api.nearby.LocationService.LocationUpdate;
import com.motolink.api.room.RoomService;
import com.motolink.api.room.RoomService.CreateRoomRequest;
import com.motolink.api.user.AppUser;
import com.motolink.api.user.AppUserRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "motolink.seed-demo", havingValue = "true", matchIfMissing = true)
public class DemoDataInitializer implements CommandLineRunner {
    private static final List<DemoUser> DEMO_USERS = List.of(
            new DemoUser("13900000001", "北四环阿凯", "Honda CB650R", 39.9068, 116.4100),
            new DemoUser("13900000002", "小满同学", "Yamaha MT-07", 39.9005, 116.4140),
            new DemoUser("13900000003", "老周慢骑", "BMW R 1250 GS", 39.9140, 116.3970));

    private final AppUserRepository users;
    private final LocationService locations;
    private final RoomService rooms;

    public DemoDataInitializer(
            AppUserRepository users,
            LocationService locations,
            RoomService rooms) {
        this.users = users;
        this.locations = locations;
        this.rooms = rooms;
    }

    @Override
    public void run(String... args) {
        for (DemoUser demo : DEMO_USERS) {
            AppUser user = users.findByPhone(demo.phone())
                    .orElseGet(() -> new AppUser(demo.phone(), demo.nickname()));
            user.updateProfile(demo.nickname(), demo.motorcycle());
            users.save(user);
        }
        refreshPresence();

        if (rooms.latestRooms().isEmpty()) {
            AppUser owner = users.findByPhone(DEMO_USERS.getFirst().phone()).orElseThrow();
            rooms.create(owner.getId(), new CreateRoomRequest("周末轻骑队", 20, true));
        }
    }

    @Scheduled(initialDelay = 25_000, fixedRate = 30_000)
    public void refreshPresence() {
        for (DemoUser demo : DEMO_USERS) {
            users.findByPhone(demo.phone()).ifPresent(user -> locations.update(
                    user.getId(),
                    new LocationUpdate(demo.latitude(), demo.longitude(), 0.0, 0.0)));
        }
    }

    private record DemoUser(
            String phone,
            String nickname,
            String motorcycle,
            double latitude,
            double longitude) {}
}
