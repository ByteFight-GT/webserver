package org.bytefight.webserver.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.TestDataFactory;
import org.bytefight.webserver.player.domain.dto.UpdatePlayerProfileDto;
import org.bytefight.webserver.player.infra.PlayerRepository;
import org.bytefight.webserver.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

@Transactional
class PrivatePlayerControllerIT extends FullStackIntegrationTestBase {
  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private TestDataFactory testDataFactory;

  @Autowired private PlayerRepository playerRepository;

  @Test
  void updateCurrentPlayerUsernameHappyPath() throws Exception {
    User user = testDataFactory.createUser();
    testDataFactory.createPlayer(user);

    UpdatePlayerProfileDto dto = UpdatePlayerProfileDto.builder().username("New_Name").build();

    mockMvc
        .perform(
            patch("/api/v1/player/me")
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

    UpdatePlayerProfileDto dto = UpdatePlayerProfileDto.builder().username("takenname").build();

    mockMvc
        .perform(
            patch("/api/v1/player/me")
                .with(user(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void updateCurrentPlayerUsernameInvalidIsRejected() throws Exception {
    User user = testDataFactory.createUser();
    testDataFactory.createPlayer(user);

    UpdatePlayerProfileDto dto =
        UpdatePlayerProfileDto.builder()
            .username("ab") // too short
            .build();

    mockMvc
        .perform(
            patch("/api/v1/player/me")
                .with(user(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest());
  }
}
