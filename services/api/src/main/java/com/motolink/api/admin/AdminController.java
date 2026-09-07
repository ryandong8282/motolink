package com.motolink.api.admin;

import com.motolink.api.ride.RideSessionRepository;
import com.motolink.api.ride.RideStatus;
import com.motolink.api.room.RideRoomRepository;
import com.motolink.api.room.RoomService;
import com.motolink.api.room.RoomService.RoomResponse;
import com.motolink.api.user.AppUser;
import com.motolink.api.user.AppUserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final AppUserRepository users;
    private final RoomService rooms;
    private final RideRoomRepository roomRepository;
    private final RideSessionRepository rides;

    public AdminController(
            AppUserRepository users,
            RoomService rooms,
            RideRoomRepository roomRepository,
            RideSessionRepository rides) {
        this.users = users;
        this.rooms = rooms;
        this.roomRepository = roomRepository;
        this.rides = rides;
    }

    @GetMapping("/overview")
    public Overview overview() {
        return new Overview(users.count(), roomRepository.count(), rides.countByStatus(RideStatus.ACTIVE));
    }

    @GetMapping("/users")
    public List<UserItem> users() {
        return users.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .limit(100)
                .map(UserItem::from)
                .toList();
    }

    @GetMapping("/rooms")
    public List<RoomResponse> rooms() {
        return rooms.latestRooms();
    }

    public record Overview(long users, long rooms, long activeRides) {}

    public record UserItem(
            UUID id,
            String phone,
            String nickname,
            String motorcycle,
            Instant createdAt) {
        static UserItem from(AppUser user) {
            return new UserItem(
                    user.getId(),
                    user.getPhone(),
                    user.getNickname(),
                    user.getMotorcycle(),
                    user.getCreatedAt());
        }
    }
}
