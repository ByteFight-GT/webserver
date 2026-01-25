package com.example.botfightwebserver.player;

import com.example.botfightwebserver.FullStackIntegrationTestBase;
import com.example.botfightwebserver.TestDataFactory;
import com.example.botfightwebserver.auth.domain.User;
import com.example.botfightwebserver.player.domain.UpdatePlayerProfileDto;
import com.example.botfightwebserver.player.infra.PlayerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class PrivatePlayerControllerIT extends FullStackIntegrationTestBase {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataFactory testDataFactory;

    @Autowired
    private PlayerRepository playerRepository;

    @Test
    void updateCurrentPlayerUsernameHappyPath() throws Exception {
        User user = testDataFactory.createUser();
        testDataFactory.createPlayer(user);

        UpdatePlayerProfileDto dto = UpdatePlayerProfileDto.builder()
                .username("New_Name")
                .build();

        mockMvc.perform(patch("/api/v1/player/me")
                        .with(user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("New_Name"));

        assertThat(playerRepository.existsByUsernameNormalized("new_name")).isTrue();
    }

    @Test
    void updateCurrentPlayerUsernameDuplicateIsRejected() throws Exception {
        User user = testDataFactory.createUser();
        testDataFactory.createPlayer(user);
        testDataFactory.createUserWithPlayer(null, "TakenName");

        UpdatePlayerProfileDto dto = UpdatePlayerProfileDto.builder()
                .username("takenname")
                .build();

        mockMvc.perform(patch("/api/v1/player/me")
                        .with(user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void updateCurrentPlayerUsernameInvalidIsRejected() throws Exception {
        User user = testDataFactory.createUser();
        testDataFactory.createPlayer(user);

        UpdatePlayerProfileDto dto = UpdatePlayerProfileDto.builder()
                .username("ab") // too short
                .build();

        mockMvc.perform(patch("/api/v1/player/me")
                        .with(user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }
}
