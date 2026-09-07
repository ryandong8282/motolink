package com.motolink.api.team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.motolink.api.common.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class TeamServiceTest {

    private final TeamService service = new TeamService(
            Clock.fixed(Instant.parse("2026-09-07T00:00:00Z"), ZoneOffset.UTC));

    @Test
    void createsAndJoinsTeam() {
        TeamService.TeamView created = service.create(
                "u_leader", "队长", "周末夜骑", "江边集合", 5, true);

        TeamService.TeamView joined = service.joinByRoomCode(
                "u_member", "新队员", created.roomCode());

        assertThat(joined.members()).hasSize(2);
        assertThat(joined.members()).extracting(TeamService.MemberView::nickname)
                .containsExactly("队长", "新队员");
    }

    @Test
    void rejectsJoinWhenTeamIsFull() {
        TeamService.TeamView created = service.create(
                "u_leader", "队长", "两人小队", "", 2, true);
        service.joinByRoomCode("u_2", "二号", created.roomCode());

        assertThatThrownBy(() -> service.joinByRoomCode("u_3", "三号", created.roomCode()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("人数已满");
    }
}
