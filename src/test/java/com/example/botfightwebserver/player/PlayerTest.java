package com.example.botfightwebserver.player;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PlayerTest {
    private static final String NAME = "TestPlayer";
    private static final String EMAIL = "test@email.com";
    private static final Long TEAM_ID = 1L;
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2024-01-01T12:00:00Z"), ZoneId.systemDefault());
    private static final int BADGE_FLAG = 3;

    @Test
    void shouldCreatePlayerWithAllFields() {
        Player player = Player.builder()
            .name(NAME)
            .email(EMAIL)
            .teamId(TEAM_ID)
            .hasTeam(true)
            .badgeBitFlags(3)
            .build();

        assertThat(player.getName()).isEqualTo(NAME);
        assertThat(player.getEmail()).isEqualTo(EMAIL);
        assertThat(player.getTeamId()).isEqualTo(TEAM_ID);
        assertThat(player.isHasTeam()).isTrue();
        assertThat(player.getBadgeBitFlags()).isEqualTo(BADGE_FLAG);
    }

    @Test
    void shouldSetCreationDateTimeOnPrePersist() {
        Player player = new Player();
        Player.setClock(FIXED_CLOCK);

        player.onCreate();

        assertThat(player.getCreationDateTime()).isEqualTo(LocalDateTime.now(FIXED_CLOCK));
    }

    @Test
    void shouldCorrectlyAddAndCheckBadges() {
        Player player = new Player();

        player.addBadge(Badge.TOURNAMENT_WINNER);
        player.addBadge(Badge.EARLY_ADOPTER);

        assertThat(player.hasBadge(Badge.TOURNAMENT_WINNER)).isTrue();
        assertThat(player.hasBadge(Badge.EARLY_ADOPTER)).isTrue();
        assertThat(player.hasBadge(Badge.TOP_CONTRIBUTOR)).isFalse();
    }

    @Test
    void shouldCorrectlyRemoveBadge() {
        Player player = new Player();

        player.addBadge(Badge.TOURNAMENT_WINNER);
        player.addBadge(Badge.EARLY_ADOPTER);
        player.removeBadge(Badge.TOURNAMENT_WINNER);

        assertThat(player.hasBadge(Badge.TOURNAMENT_WINNER)).isFalse();
        assertThat(player.hasBadge(Badge.EARLY_ADOPTER)).isTrue();
    }

    @Test
    void shouldReturnCorrectBadgeList() {
        Player player = new Player();

        player.addBadge(Badge.TOURNAMENT_WINNER);
        player.addBadge(Badge.EARLY_ADOPTER);

        assertThat(player.getBadgeList())
            .containsExactlyInAnyOrder(
                Badge.TOURNAMENT_WINNER.getDisplayName(),
                Badge.EARLY_ADOPTER.getDisplayName()
            );
    }

    @Test
    void shouldReturnEmptyBadgeListWhenNoBadges() {
        Player player = new Player();

        assertThat(player.getBadgeList()).isEmpty();
    }

    @Test
    void shouldHaveZeroBadgesByDefault() {
        Player player = new Player();
        assertThat(player.getBadgeBitFlags()).isZero();

        Player builderPlayer = Player.builder().build();
        assertThat(builderPlayer.getBadgeBitFlags()).isZero();
    }
}