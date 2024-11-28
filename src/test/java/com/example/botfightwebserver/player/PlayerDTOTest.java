package com.example.botfightwebserver.player;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class PlayerDTOTest {

    @Test
    void fromEntityShouldMapAllFields() {
        Player player = Player.builder()
            .id(1L)
            .name("Test")
            .email("test@email.com")
            .teamId(2L)
            .creationDateTime(LocalDateTime.of(2024, 1, 1, 12, 0))
            .build();

        PlayerDTO dto = PlayerDTO.fromEntity(player);

        assertThat(dto)
            .extracting("id", "name", "email", "teamId", "creationDateTime")
            .containsExactly(1L, "Test", "test@email.com", 2L,
                LocalDateTime.of(2024, 1, 1, 12, 0));
    }
}