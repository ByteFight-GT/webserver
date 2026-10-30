package org.bytefight.webserver.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.TestDataFactory;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.profile.domain.Profile;
import org.bytefight.webserver.profile.infra.ProfileRepository;
import org.bytefight.webserver.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ProfileControllerIT extends FullStackIntegrationTestBase {
  @Autowired private MockMvc mockMvc;

  @Autowired private TestDataFactory testDataFactory;

  @Autowired private ProfileRepository profileRepository;

  @Test
  void authenticatedPlayerCanCreateReadUpdateAndDeleteProfile() throws Exception {
    User user = testDataFactory.createUser();
    Player player = testDataFactory.createPlayer(user, "profile-owner");
    String createRequest =
        """
        {
          "description": "Looking for teammates",
          "major": "Computer Science",
          "school": "Georgia Tech",
          "graduationYear": 2027
        }
        """;

    mockMvc
        .perform(
            post("/api/v1/profile/me")
                .with(user(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.username").value("profile-owner"))
        .andExpect(jsonPath("$.major").value("Computer Science"))
        .andExpect(jsonPath("$.graduationYear").value(2027))
        .andExpect(jsonPath("$.email").doesNotExist());

    mockMvc
        .perform(get("/api/v1/profile/me").with(user(user)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.school").value("Georgia Tech"));

    mockMvc
        .perform(
            put("/api/v1/profile/me")
                .with(user(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "description": "Updated description",
                      "major": "Computer Engineering",
                      "graduationYear": 2028
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.major").value("Computer Engineering"))
        .andExpect(jsonPath("$.school").doesNotExist());

    assertThat(profileRepository.findByPlayerId(player.getId())).isPresent();

    mockMvc
        .perform(delete("/api/v1/profile/me").with(user(user)))
        .andExpect(status().isNoContent());
    assertThat(profileRepository.findByPlayerId(player.getId())).isEmpty();
  }

  @Test
  void profileCreationRequiresAtLeastOneDetailAndRejectsDuplicates() throws Exception {
    User user = testDataFactory.createUser();
    testDataFactory.createPlayer(user);

    mockMvc
        .perform(
            post("/api/v1/profile/me")
                .with(user(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            post("/api/v1/profile/me")
                .with(user(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"major\":\"Math\"}"))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/v1/profile/me")
                .with(user(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"major\":\"Physics\"}"))
        .andExpect(status().isConflict());
  }

  @Test
  void publicProfileSearchSupportsTextFiltersAndPagination() throws Exception {
    Profile ada = createProfile("ada_lovelace", "Computer Science", "Georgia Tech", 2027);
    createProfile("grace_hopper", "Computer Engineering", "Georgia Tech", 2028);

    mockMvc
        .perform(
            get("/api/v1/public/profile")
                .param("query", "computer")
                .param("major", "computer")
                .param("school", "georgia")
                .param("page", "0")
                .param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].username").value(ada.getPlayer().getUsername()))
        .andExpect(jsonPath("$.totalElements").value(2));
  }

  @Test
  void suggestionsRankProfilesByNumberOfMatchingFieldsAndExcludeCurrentPlayer() throws Exception {
    User currentUser = testDataFactory.createUser();
    Player currentPlayer = testDataFactory.createPlayer(currentUser, "current-player");
    saveProfile(currentPlayer, "Current player", "Computer Science", "Georgia Tech", 2027);
    createProfile("exact_match", "Computer Science", "Georgia Tech", 2027);
    createProfile("two_matches", "Computer Science", "Georgia Tech", 2028);
    createProfile("one_match", "Computer Science", "Emory", 2028);

    mockMvc
        .perform(get("/api/v1/profile/me/suggestions").with(user(currentUser)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(3))
        .andExpect(jsonPath("$.content[0].profile.username").value("exact_match"))
        .andExpect(jsonPath("$.content[0].matchingFields.length()").value(3))
        .andExpect(jsonPath("$.content[1].profile.username").value("two_matches"))
        .andExpect(jsonPath("$.content[1].matchingFields.length()").value(2))
        .andExpect(jsonPath("$.content[2].profile.username").value("one_match"))
        .andExpect(jsonPath("$.content[2].matchingFields.length()").value(1));
  }

  @Test
  void privateProfileEndpointsRequireAuthentication() throws Exception {
    mockMvc.perform(get("/api/v1/profile/me")).andExpect(status().isUnauthorized());
  }

  private Profile createProfile(
      String username, String major, String school, Integer graduationYear) {
    Player player = testDataFactory.createUserWithPlayer(null, username);
    return saveProfile(player, username, major, school, graduationYear);
  }

  private Profile saveProfile(
      Player player, String description, String major, String school, Integer graduationYear) {
    Profile profile = new Profile();
    profile.setPlayer(player);
    profile.setDescription(description);
    profile.setMajor(major);
    profile.setSchool(school);
    profile.setGraduationYear(graduationYear);
    return profileRepository.save(profile);
  }
}
