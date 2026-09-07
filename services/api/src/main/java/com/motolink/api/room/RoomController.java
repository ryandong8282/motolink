package com.motolink.api.room;

import com.motolink.api.room.FloorService.FloorResponse;
import com.motolink.api.room.RoomService.CreateRoomRequest;
import com.motolink.api.room.RoomService.JoinRoomRequest;
import com.motolink.api.room.RoomService.RoomResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {
    private final RoomService rooms;
    private final FloorService floor;

    public RoomController(RoomService rooms, FloorService floor) {
        this.rooms = rooms;
        this.floor = floor;
    }

    @PostMapping
    public RoomResponse create(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody CreateRoomRequest request) {
        return rooms.create(userId, request);
    }

    @PostMapping("/join")
    public RoomResponse join(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody JoinRoomRequest request) {
        return rooms.join(userId, request.roomCode());
    }

    @GetMapping("/{roomId}")
    public RoomResponse get(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID roomId) {
        return rooms.get(roomId, userId);
    }

    @PostMapping("/{roomId}/floor/request")
    public FloorResponse requestFloor(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID roomId) {
        rooms.requireMember(roomId, userId);
        return floor.request(roomId, userId);
    }

    @PostMapping("/{roomId}/floor/heartbeat")
    public FloorResponse heartbeat(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID roomId) {
        rooms.requireMember(roomId, userId);
        return floor.heartbeat(roomId, userId);
    }

    @PostMapping("/{roomId}/floor/release")
    public FloorResponse releaseFloor(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID roomId) {
        return floor.release(roomId, userId);
    }
}
