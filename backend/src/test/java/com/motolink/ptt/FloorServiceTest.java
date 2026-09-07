package com.motolink.ptt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.motolink.team.TeamService;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FloorServiceTest {

    @Test
    void onlyOneMemberOwnsTheFloorAtATime() {
        TeamService teams = new TeamService();
        UUID captain = UUID.randomUUID();
        UUID rider = UUID.randomUUID();
        TeamService.TeamView team = teams.create(new TeamService.CreateTeam(
                "Test team", captain, "", 8, false));
        teams.join(team.roomCode(), rider);

        FloorService floors = new FloorService(teams, 20);
        FloorService.FloorLease firstLease = floors.claim(team.id(), captain);

        assertThat(firstLease.userId()).isEqualTo(captain);
        assertThatThrownBy(() -> floors.claim(team.id(), rider))
                .isInstanceOf(FloorService.FloorBusyException.class);

        floors.release(team.id(), captain, firstLease.leaseToken());
        FloorService.FloorLease secondLease = floors.claim(team.id(), rider);
        assertThat(secondLease.userId()).isEqualTo(rider);
    }
}
