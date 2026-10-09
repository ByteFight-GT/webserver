package org.bytefight.webserver.notification;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.TestDataFactory;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.notification.domain.NotificationPriority;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.user.domain.User;
import org.bytefight.webserver.user.infra.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class NotificationControllerIT extends FullStackIntegrationTestBase {
  private static final String BASE = "/api/v1/notification";

  @Autowired private MockMvc mockMvc;

  @Autowired private TestDataFactory testDataFactory;

  @Autowired private UserRepository userRepository;

  private User user;
  private Competition myCompetition;
  private Competition otherCompetition;

  /** The user is on a team in {@code myCompetition}; nothing exists yet that they have checked. */
  @BeforeEach
  void setUp() {
    Player player = testDataFactory.createUserWithPlayer();
    user = player.getUser();
    user.setLastNotificationsChecked(Instant.EPOCH);
    userRepository.save(user);

    myCompetition = testDataFactory.createCompetition();
    otherCompetition = testDataFactory.createCompetition();
    Team team = testDataFactory.createTeam(myCompetition);
    testDataFactory.addTeamMember(team, player);
  }

  @Test
  void feedHasSiteWideAndOwnCompetitionsOnly() throws Exception {
    testDataFactory.createNotification("Site", null, NotificationPriority.NORMAL, null);
    testDataFactory.createNotification("Mine", myCompetition, NotificationPriority.NORMAL, null);
    testDataFactory.createNotification(
        "Other", otherCompetition, NotificationPriority.NORMAL, null);

    mockMvc
        .perform(get(BASE).with(user(user)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.content[*].title", containsInAnyOrder("Site", "Mine")))
        .andExpect(jsonPath("$.page.totalElements").value(2));
  }

  @Test
  void feedIsNewestFirstAndCarriesCompetitionAndUnread() throws Exception {
    testDataFactory.createNotification("First", null, NotificationPriority.NORMAL, null);
    testDataFactory.createNotification("Second", myCompetition, NotificationPriority.NORMAL, null);

    mockMvc
        .perform(get(BASE).with(user(user)))
        .andExpect(jsonPath("$.content[0].title").value("Second"))
        .andExpect(jsonPath("$.content[0].competitionSlug").value(myCompetition.getSlug()))
        .andExpect(jsonPath("$.content[0].competitionName").value(myCompetition.getName()))
        .andExpect(jsonPath("$.content[0].unread").value(true))
        .andExpect(jsonPath("$.content[1].title").value("First"))
        .andExpect(jsonPath("$.content[1].competitionSlug").doesNotExist());
  }

  @Test
  void feedExcludesExpired() throws Exception {
    testDataFactory.createNotification(
        "Expired", null, NotificationPriority.NORMAL, Instant.now().minusSeconds(60));
    testDataFactory.createNotification(
        "Live", null, NotificationPriority.NORMAL, Instant.now().plusSeconds(3600));

    mockMvc
        .perform(get(BASE).with(user(user)))
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].title").value("Live"));
  }

  @Test
  void feedExcludesCompetitionsOfDeletedTeams() throws Exception {
    Player player = testDataFactory.createUserWithPlayer();
    Team deletedTeam = testDataFactory.createTeam(otherCompetition, null, true);
    testDataFactory.addTeamMember(deletedTeam, player);
    testDataFactory.createNotification(
        "Other", otherCompetition, NotificationPriority.NORMAL, null);

    mockMvc
        .perform(get(BASE).with(user(player.getUser())))
        .andExpect(jsonPath("$.content", hasSize(0)));
  }

  @Test
  void internalCompetitionFollowsTheSameMembershipRule() throws Exception {
    Competition internal = testDataFactory.createInternalCompetition("internal-notif");
    Player member = testDataFactory.createUserWithPlayer();
    testDataFactory.addTeamMember(testDataFactory.createTeam(internal), member);
    testDataFactory.createNotification("Internal", internal, NotificationPriority.NORMAL, null);

    mockMvc
        .perform(get(BASE).with(user(member.getUser())))
        .andExpect(jsonPath("$.content[*].title", containsInAnyOrder("Internal")));
    mockMvc.perform(get(BASE).with(user(user))).andExpect(jsonPath("$.content", hasSize(0)));
  }

  @Test
  void userWithoutPlayerSeesSiteWideOnly() throws Exception {
    User noPlayer = testDataFactory.createUser();
    testDataFactory.createNotification("Site", null, NotificationPriority.NORMAL, null);
    testDataFactory.createNotification("Mine", myCompetition, NotificationPriority.NORMAL, null);

    mockMvc
        .perform(get(BASE).with(user(noPlayer)))
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].title").value("Site"));
  }

  @Test
  void unreadCountDropsToZeroAfterMarkReadAndCountsNewOnes() throws Exception {
    testDataFactory.createNotification("Site", null, NotificationPriority.NORMAL, null);
    testDataFactory.createNotification("Mine", myCompetition, NotificationPriority.NORMAL, null);
    testDataFactory.createNotification(
        "Other", otherCompetition, NotificationPriority.NORMAL, null);

    mockMvc
        .perform(get(BASE + "/unread-count").with(user(user)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.count").value(2));

    mockMvc.perform(post(BASE + "/mark-read").with(user(user))).andExpect(status().isNoContent());

    mockMvc
        .perform(get(BASE + "/unread-count").with(user(user)))
        .andExpect(jsonPath("$.count").value(0));
    mockMvc
        .perform(get(BASE).with(user(user)))
        .andExpect(jsonPath("$.content[0].unread").value(false));

    testDataFactory.createNotification("Later", null, NotificationPriority.NORMAL, null);

    mockMvc
        .perform(get(BASE + "/unread-count").with(user(user)))
        .andExpect(jsonPath("$.count").value(1));
  }

  @Test
  void urgentReturnsActiveUrgentForTheUsersAudience() throws Exception {
    testDataFactory.createNotification("Normal", null, NotificationPriority.NORMAL, null);
    testDataFactory.createNotification("Site urgent", null, NotificationPriority.URGENT, null);
    testDataFactory.createNotification(
        "My urgent", myCompetition, NotificationPriority.URGENT, null);
    testDataFactory.createNotification(
        "Other urgent", otherCompetition, NotificationPriority.URGENT, null);
    testDataFactory.createNotification(
        "Expired urgent", null, NotificationPriority.URGENT, Instant.now().minusSeconds(60));

    mockMvc
        .perform(get(BASE + "/urgent").with(user(user)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].title", containsInAnyOrder("Site urgent", "My urgent")));
  }

  @Test
  void publicUrgentIsSiteWideOnlyAndNeedsNoLogin() throws Exception {
    testDataFactory.createNotification("Site urgent", null, NotificationPriority.URGENT, null);
    testDataFactory.createNotification("Site normal", null, NotificationPriority.NORMAL, null);
    testDataFactory.createNotification(
        "My urgent", myCompetition, NotificationPriority.URGENT, null);

    mockMvc
        .perform(get("/api/v1/public/notification/urgent"))
        .andExpect(status().isOk())
        .andExpect(header().doesNotExist("Content-Range"))
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].title").value("Site urgent"))
        .andExpect(jsonPath("$[0].unread").value(false));
  }

  @Test
  void anonymousCannotReadTheFeed() throws Exception {
    mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
  }
}
