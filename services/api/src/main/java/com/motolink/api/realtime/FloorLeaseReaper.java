package com.motolink.api.realtime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * MVP in-memory lease reaper. It turns a server-side lease timeout into a
 * realtime event so a client cannot continue publishing after losing the floor.
 */
@Component
public class FloorLeaseReaper {

    private final FloorService floorService;
    private final TeamSocketHandler teamSocketHandler;

    public FloorLeaseReaper(
            FloorService floorService,
            TeamSocketHandler teamSocketHandler) {
        this.floorService = floorService;
        this.teamSocketHandler = teamSocketHandler;
    }

    @Scheduled(fixedDelayString = "${motolink.floor-reaper-interval-ms:1000}")
    void expireLeases() {
        floorService.expireStale().forEach(teamSocketHandler::broadcastExpiredFloor);
    }
}
