package org.bytefight.webserver.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.TestDataFactory;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.notification.domain.Notification;
import org.bytefight.webserver.notification.domain.NotificationPriority;
import org.bytefight.webserver.notification.infra.NotificationRepository;
import org.bytefight.webserver.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

@Transactional
class AdminNotificationControllerIT extends FullStackIntegrationTestBase {
  private static final String BASE = "/api/v1/admin/notification";

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private TestDataFactory testDataFactory;

  @Autowired private NotificationRepository notificationRepository;

  private User admin;

  @BeforeEach
  void setUp() {
    admin = testDataFactory.createUser(null, true);
  }

  @Test
  void createReturns201WithDefaultsAndAuditFields() throws Exception {
    Competition competition = testDataFactory.createCompetition();
    Map<String, Object> body = createBody("Deadline moved");
    body.put("competitionId", competition.getId());

    mockMvc
        .perform(json(post(BASE), body).with(asAdmin()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.title").value("Deadline moved"))
        .andExpect(jsonPath("$.priority").value("NORMAL"))
        .andExpect(jsonPath("$.withEmail").value(false))
        .andExpect(jsonPath("$.competitionId").value(competition.getId()))
        .andExpect(jsonPath("$.expireAt").value(nullValue()))
        .andExpect(jsonPath("$.createdByUserId").value(admin.getId()))
        .andExpect(jsonPath("$.createdByEmail").value(admin.getEmail()));
  }

  @Test
  void createSiteWideUrgentWithExpiry() throws Exception {
    Instant expireAt = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
    Map<String, Object> body = createBody("Maintenance tonight");
    body.put("priority", "URGENT");
    body.put("withEmail", true);
    body.put("expireAt", expireAt.toString());

    mockMvc
        .perform(json(post(BASE), body).with(asAdmin()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.priority").value("URGENT"))
        .andExpect(jsonPath("$.withEmail").value(true))
        .andExpect(jsonPath("$.competitionId").value(nullValue()))
        .andExpect(jsonPath("$.expireAt").value(expireAt.toString()));
  }

  @Test
  void getReturnsOneNotification() throws Exception {
    Notification notification =
        testDataFactory.createNotification("Hello", null, NotificationPriority.NORMAL, null);

    mockMvc
        .perform(get(BASE + "/{id}", notification.getId()).with(asAdmin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(notification.getId()))
        .andExpect(jsonPath("$.title").value("Hello"));
  }

  @Test
  void getUnknownIdIs404() throws Exception {
    mockMvc.perform(get(BASE + "/{id}", 999_999L).with(asAdmin())).andExpect(status().isNotFound());
  }

  @Test
  void listIsNewestFirstWithContentRange() throws Exception {
    testDataFactory.createNotification("Older", null, NotificationPriority.NORMAL, null);
    testDataFactory.createNotification("Newer", null, NotificationPriority.URGENT, null);

    mockMvc
        .perform(get(BASE).param("range", "[0,9]").with(asAdmin()))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Range", "notification 0-1/2"))
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].title").value("Newer"))
        .andExpect(jsonPath("$[1].title").value("Older"));
  }

  @Test
  void listFiltersByCompetitionPriorityAndSearch() throws Exception {
    Competition competition = testDataFactory.createCompetition();
    testDataFactory.createNotification(
        "Comp normal", competition, NotificationPriority.NORMAL, null);
    testDataFactory.createNotification(
        "Comp urgent", competition, NotificationPriority.URGENT, null);
    testDataFactory.createNotification("Site urgent", null, NotificationPriority.URGENT, null);

    mockMvc
        .perform(
            get(BASE)
                .param("filter", "{\"competitionId\":" + competition.getId() + "}")
                .with(asAdmin()))
        .andExpect(jsonPath("$", hasSize(2)));

    mockMvc
        .perform(get(BASE).param("filter", "{\"priority\":\"URGENT\"}").with(asAdmin()))
        .andExpect(jsonPath("$", hasSize(2)));

    mockMvc
        .perform(get(BASE).param("filter", "{\"q\":\"SITE\"}").with(asAdmin()))
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].title").value("Site urgent"));
  }

  @Test
  void patchUpdatesOnlyGivenFields() throws Exception {
    Competition competition = testDataFactory.createCompetition();
    Notification notification =
        testDataFactory.createNotification(
            "Old title", competition, NotificationPriority.NORMAL, null);

    mockMvc
        .perform(
            json(patch(BASE + "/{id}", notification.getId()), Map.of("title", "New title"))
                .with(asAdmin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("New title"))
        .andExpect(jsonPath("$.body").value("Body of Old title"))
        .andExpect(jsonPath("$.competitionId").value(competition.getId()))
        .andExpect(jsonPath("$.updatedByUserId").value(admin.getId()));
  }

  @Test
  void patchWithExplicitNullsMakesSiteWideAndNeverExpiring() throws Exception {
    Competition competition = testDataFactory.createCompetition();
    Notification notification =
        testDataFactory.createNotification(
            "Scoped", competition, NotificationPriority.NORMAL, Instant.now().plusSeconds(3600));
    Map<String, Object> body = new HashMap<>();
    body.put("competitionId", null);
    body.put("expireAt", null);

    mockMvc
        .perform(json(patch(BASE + "/{id}", notification.getId()), body).with(asAdmin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.competitionId").value(nullValue()))
        .andExpect(jsonPath("$.expireAt").value(nullValue()));
  }

  @Test
  void deleteReturnsTheDeletedRecord() throws Exception {
    Notification notification =
        testDataFactory.createNotification("Doomed", null, NotificationPriority.NORMAL, null);

    mockMvc
        .perform(delete(BASE + "/{id}", notification.getId()).with(asAdmin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(notification.getId()));

    assertThat(notificationRepository.findById(notification.getId())).isEmpty();
  }

  @Test
  void unknownCompetitionIs404() throws Exception {
    Map<String, Object> body = createBody("Nowhere");
    body.put("competitionId", 999_999L);

    mockMvc.perform(json(post(BASE), body).with(asAdmin())).andExpect(status().isNotFound());
  }

  @Test
  void createValidatesTitleBodyAndExpiry() throws Exception {
    Map<String, Object> blankTitle = createBody("");
    mockMvc
        .perform(json(post(BASE), blankTitle).with(asAdmin()))
        .andExpect(status().isBadRequest());

    Map<String, Object> longTitle = createBody("x".repeat(201));
    mockMvc.perform(json(post(BASE), longTitle).with(asAdmin())).andExpect(status().isBadRequest());

    Map<String, Object> noBody = createBody("No body");
    noBody.remove("body");
    mockMvc.perform(json(post(BASE), noBody).with(asAdmin())).andExpect(status().isBadRequest());

    Map<String, Object> pastExpiry = createBody("Already over");
    pastExpiry.put("expireAt", Instant.now().minusSeconds(60).toString());
    mockMvc
        .perform(json(post(BASE), pastExpiry).with(asAdmin()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void nonAdminGets403() throws Exception {
    User nonAdmin = testDataFactory.createUser();
    Notification notification =
        testDataFactory.createNotification("Hello", null, NotificationPriority.NORMAL, null);

    mockMvc.perform(get(BASE).with(user(nonAdmin))).andExpect(status().isForbidden());
    mockMvc
        .perform(json(post(BASE), createBody("Sneaky")).with(user(nonAdmin)))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            json(patch(BASE + "/{id}", notification.getId()), Map.of("title", "Sneaky"))
                .with(user(nonAdmin)))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(delete(BASE + "/{id}", notification.getId()).with(user(nonAdmin)))
        .andExpect(status().isForbidden());
  }

  /** A real {@link User} principal with ROLE_ADMIN, as the JWT converter would build it. */
  private RequestPostProcessor asAdmin() {
    return authentication(
        new UsernamePasswordAuthenticationToken(
            admin, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
  }

  private MockHttpServletRequestBuilder json(
      MockHttpServletRequestBuilder request, Map<String, Object> body) throws Exception {
    return request
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(body));
  }

  private static Map<String, Object> createBody(String title) {
    Map<String, Object> body = new HashMap<>();
    body.put("title", title);
    body.put("body", "Some **markdown** body");
    return body;
  }
}
