package com.example.botfightwebserver.player;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlayerDTOTest {

    @Test
    void fromEntityShouldMapAllFields() {
        Player player = Player.builder()
            .id(1L)
            .name("Test")
            .email("test@email.com")
            .teamId(2L)
            .hasTeam(true)
            .creationDateTime(LocalDateTime.of(2024, 1, 1, 12, 0))
            .badgeBitFlags(3)
            .build();

        PlayerDTO dto = PlayerDTO.fromEntity(player);

        assertThat(dto)
            .extracting("id", "name", "email", "teamId", "hasTeam", "creationDateTime", "badges")
            .containsExactly(
                1L,
                "Test",
                "test@email.com",
                2L,
                true,
                LocalDateTime.of(2024, 1, 1, 12, 0),
                List.of("Tournament Winner", "Early Adopter")
            );
    }
}