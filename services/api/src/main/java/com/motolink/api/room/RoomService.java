package com.motolink.api.room;

import com.motolink.api.common.DomainException;
import com.motolink.api.user.AppUser;
import com.motolink.api.user.UserService;
import jakarta.validation.constraints.NotBlank;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {
    private final RideRoomRepository rooms;
    private final RoomMemberRepository members;
    private final UserService users;
    private final SecureRandom random = new SecureRandom();

    public RoomService(
            RideRoomRepository rooms,
            RoomMemberRepository members,
            UserService users) {
        this.rooms = rooms;
        this.members = members;
        this.users = users;
    }

    @Transactional
    public RoomResponse create(UUID ownerId, CreateRoomRequest request) {
        AppUser owner = users.require(ownerId);
        RideRoom room = new RideRoom(
                nextRoomCode(),
                request.name(),
                request.maxMembers(),
                request.publicRoom());
        room.addMember(new RoomMember(owner, RoomRole.OWNER));
        return toResponse(rooms.save(room));
    }

    @Transactional
    public RoomResponse join(UUID userId, String roomCode) {
        AppUser user = users.require(userId);
        RideRoom room = rooms.findByRoomCode(roomCode)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "车队房间不存在"));
        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new DomainException(HttpStatus.CONFLICT, "车队房间已关闭");
        }
        if (!members.existsByRoom_IdAndUser_Id(room.getId(), userId)) {
            if (members.countByRoom_Id(room.getId()) >= room.getMaxMembers()) {
                throw new DomainException(HttpStatus.CONFLICT, "车队人数已满");
            }
            room.addMember(new RoomMember(user, RoomRole.MEMBER));
            rooms.save(room);
        }
        return toResponse(room);
    }

    @Transactional(readOnly = true)
    public RoomResponse get(UUID roomId, UUID userId) {
        requireMember(roomId, userId);
        RideRoom room = rooms.findOneById(roomId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "车队房间不存在"));
        return toResponse(room);
    }

    @Transactional(readOnly = true)
    public void requireMember(UUID roomId, UUID userId) {
        if (!members.existsByRoom_IdAndUser_Id(roomId, userId)) {
            throw new DomainException(HttpStatus.FORBIDDEN, "当前用户不在该车队中");
        }
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> latestRooms() {
        return rooms.findTop50ByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    private String nextRoomCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String code = "%06d".formatted(random.nextInt(1_000_000));
            if (!rooms.existsByRoomCode(code)) return code;
        }
        throw new DomainException(HttpStatus.CONFLICT, "暂时无法生成房间号，请重试");
    }

    private RoomResponse toResponse(RideRoom room) {
        List<MemberResponse> memberResponses = room.getMembers().stream()
                .map(member -> new MemberResponse(
                        member.getUser().getId(),
                        member.getUser().getNickname(),
                        member.getRole().name(),
                        true))
                .toList();
        return new RoomResponse(
                room.getId(),
                room.getRoomCode(),
                room.getName(),
                room.getMaxMembers(),
                room.isPublicRoom(),
                room.getStatus().name(),
                room.getCreatedAt(),
                memberResponses);
    }

    public record CreateRoomRequest(String name, int maxMembers, boolean publicRoom) {
        public CreateRoomRequest {
            if (name == null || name.isBlank()) name = "临时骑行队";
            if (maxMembers < 2 || maxMembers > 50) maxMembers = 20;
        }
    }

    public record JoinRoomRequest(@NotBlank String roomCode) {}

    public record MemberResponse(UUID userId, String nickname, String role, boolean online) {}

    public record RoomResponse(
            UUID id,
            String roomCode,
            String name,
            int maxMembers,
            boolean publicRoom,
            String status,
            Instant createdAt,
            List<MemberResponse> members) {}
}
