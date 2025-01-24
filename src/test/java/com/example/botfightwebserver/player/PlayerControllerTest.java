package com.example.botfightwebserver.player;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

@WebMvcTest(PlayerController.class)
class PlayerControllerTest {

    private static final UUID TEST_AUTH_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlayerService playerService;

    private static final Player TEST_PLAYER = Player.builder()
        .id(1L)
        .name("Test")
        .email("test@email.com")
        .authId(TEST_AUTH_ID)
        .build();

    @Test
    void shouldGetAllPlayers() throws Exception {
        List<Player> players = List.of(TEST_PLAYER);
        when(playerService.getPlayers()).thenReturn(players);

        mockMvc.perform(get("/api/v1/player/players"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].name").value("Test"))
            .andExpect(jsonPath("$[0].email").value("test@email.com"))
            .andExpect(jsonPath("$[0].authId").value(TEST_AUTH_ID.toString()));

        verify(playerService).getPlayers();
    }

    @Test
    void shouldGetPlayerById() throws Exception {
        when(playerService.getPlayer(1L)).thenReturn(TEST_PLAYER);

        mockMvc.perform(get("/api/v1/player/player")
                .param("id", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Test"))
            .andExpect(jsonPath("$.email").value("test@email.com"))
            .andExpect(jsonPath("$.authId").value(TEST_AUTH_ID.toString()));

        verify(playerService).getPlayer(1L);
    }

    @Test
    void shouldCreatePlayer() throws Exception {
        when(playerService.createPlayer("Test", "test@email.com", TEST_AUTH_ID, null))
            .thenReturn(TEST_PLAYER);

        mockMvc.perform(post("/api/v1/player/create")
                .param("name", "Test")
                .param("email", "test@email.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Test"))
            .andExpect(jsonPath("$.email").value("test@email.com"))
            .andExpect(jsonPath("$.authId").value(TEST_AUTH_ID.toString()));

        verify(playerService).createPlayer("Test", "test@email.com", TEST_AUTH_ID,null);
    }

    @Test
    void shouldAssignTeam() throws Exception {
        Player playerWithTeam =  Player.builder()
            .id(1L)
            .teamId(2L)
            .name("Test")
            .email("test@email.com")
            .authId(TEST_AUTH_ID)
            .build();

        when(playerService.setPlayerTeam(TEST_AUTH_ID, 2L)).thenReturn(playerWithTeam);

        mockMvc.perform(post("/api/v1/player/team")
                .param("playerId", "1")
                .param("teamId", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.teamId").value(2));

        verify(playerService).setPlayerTeam(TEST_AUTH_ID, 2L);
    }
}