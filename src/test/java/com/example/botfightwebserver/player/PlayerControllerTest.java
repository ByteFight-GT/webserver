package com.example.botfightwebserver.player;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlayerController.class)
class PlayerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlayerService playerService;

    @Test
    void testGetPlayers() throws Exception {
        Player player1 = Player.builder().id(1L).email("tkwok123@gmail.com").name("tyler").build();
        Player player2 = Player.builder().id(2L).email("bkwok123@gmail.com").name("ben").build();
        List<Player> players = List.of(player1, player2);

        when(playerService.getPlayers()).thenReturn(players);

        mockMvc.perform(get("/api/v1/player/players"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].name").value("tyler"))
            .andExpect(jsonPath("$[0].email").value("tkwok123@gmail.com"))
            .andExpect(jsonPath("$[1].id").value(2L))
            .andExpect(jsonPath("$[1].name").value("ben"))
            .andExpect(jsonPath("$[1].email").value("bkwok123@gmail.com"));
    }

    @Test
    void testGetPlayersEmpty() throws Exception {
        List<Player> emptyPlayers = List.of();
        when(playerService.getPlayers()).thenReturn(emptyPlayers);

        mockMvc.perform(get("/api/v1/player/players"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void testCreatePlayer() throws Exception {
        Player newPlayer =
            Player.builder()
                .id(1L)
                .email("tkwok123@gmail.com")
                .name("tyler")
                .glicko(1200.0)
                .matchesPlayed(0)
                .numberLosses(0)
                .numberWins(0)
                .numberDraws(0).build();

        when(playerService.createPlayer("tyler", "tkwok123@gmail.com")).thenReturn(newPlayer);

        mockMvc.perform(post("/api/v1/player")
                .param("email", "tkwok123@gmail.com")
                .param("name", "tyler")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.name").value("tyler"))
            .andExpect(jsonPath("$.email").value("tkwok123@gmail.com"))
            .andExpect(jsonPath("$.matchesPlayed").value(0))
            .andExpect(jsonPath("$.numberLosses").value(0))
            .andExpect(jsonPath("$.numberWins").value(0))
            .andExpect(jsonPath("$.numberDraws").value(0));
    }

    @Test
    void testCreatePlayerEmailExists() throws Exception {
        String email = "tkwok123@gmail.com";
        String name = "tyler";
        when(playerService.createPlayer("tyler", "tkwok123@gmail.com")).thenThrow(
            new IllegalArgumentException("Player with email " + email + " already exists"));

        mockMvc.perform(post("/api/v1/player")
            .param("email", "tkwok123@gmail.com")
            .param("name", "tyler")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Player with email " + email + " already exists")));
    }

}