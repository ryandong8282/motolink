package com.motolink.api.room;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RideRoomRepository extends JpaRepository<RideRoom, UUID> {
    boolean existsByRoomCode(String roomCode);

    @EntityGraph(attributePaths = {"members", "members.user"})
    Optional<RideRoom> findOneById(UUID id);

    @EntityGraph(attributePaths = {"members", "members.user"})
    Optional<RideRoom> findByRoomCode(String roomCode);

    @EntityGraph(attributePaths = {"members", "members.user"})
    List<RideRoom> findTop50ByOrderByCreatedAtDesc();
}
