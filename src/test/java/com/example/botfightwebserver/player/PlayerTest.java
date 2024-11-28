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

    @Test
    void shouldCreatePlayerWithAllFields() {
        Player player = Player.builder()
            .name(NAME)
            .email(EMAIL)
            .teamId(TEAM_ID)
            .build();

        assertThat(player.getName()).isEqualTo(NAME);
        assertThat(player.getEmail()).isEqualTo(EMAIL);
        assertThat(player.getTeamId()).isEqualTo(TEAM_ID);
    }

    @Test
    void shouldSetCreationDateTimeOnPrePersist() {
        Player player = new Player();
        Player.setClock(FIXED_CLOCK);

        player.onCreate();

        assertThat(player.getCreationDateTime()).isEqualTo(LocalDateTime.now(FIXED_CLOCK));
    }
}