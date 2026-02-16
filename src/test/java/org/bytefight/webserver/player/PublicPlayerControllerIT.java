package org.bytefight.webserver.player;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.TestDataFactory;
import org.bytefight.webserver.auth.domain.User;
import org.bytefight.webserver.player.domain.Player;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class PublicPlayerControllerIT extends FullStackIntegrationTestBase {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestDataFactory testDataFactory;

    @Test
    void getPublicPlayerByUuidReturnsPlayer() throws Exception {
        Player player = testDataFactory.createUserWithPlayer();
        User user = player.getUser();

        mockMvc.perform(get("/api/v1/public/player/{uuid}", user.getUuid().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(user.getUuid().toString()))
                .andExpect(jsonPath("$.username").value(player.getUsername()));
    }

    @Test
    void getPublicPlayerByUuidNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/public/player/{uuid}", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void checkUsernameAvailabilityReturnsUnavailableWhenTaken() throws Exception {
        testDataFactory.createUserWithPlayer(null, "TakenName");

        mockMvc.perform(get("/api/v1/public/player/check-username/{username}", "takenname"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void checkUsernameAvailabilityReturnsAvailableWhenFree() throws Exception {
        mockMvc.perform(get("/api/v1/public/player/check-username/{username}", "fresh_name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }
}
