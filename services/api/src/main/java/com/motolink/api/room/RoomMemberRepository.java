package com.motolink.api.room;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomMemberRepository extends JpaRepository<RoomMember, UUID> {
    boolean existsByRoom_IdAndUser_Id(UUID roomId, UUID userId);
    long countByRoom_Id(UUID roomId);
}
