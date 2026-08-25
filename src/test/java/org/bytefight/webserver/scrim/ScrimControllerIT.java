package org.bytefight.webserver.scrim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.TestDataFactory;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.gamematch.application.GameMatchResultHandler;
import org.bytefight.webserver.gamematch.domain.DefaultLadders;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchReason;
import org.bytefight.webserver.gamematch.domain.MatchStatus;
import org.bytefight.webserver.gamematch.domain.dto.GameMatchJob;
import org.bytefight.webserver.gamematch.domain.dto.GameMatchResult;
import org.bytefight.webserver.gamematch.infra.GameMatchRepository;
import org.bytefight.webserver.glicko.infra.TeamStatsRepository;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.scrim.infra.ScrimProperties;
import org.bytefight.webserver.storage.domain.FileRecord;
import org.bytefight.webserver.storage.infra.FileRecordRepository;
import org.bytefight.webserver.submission.domain.Submission;
import org.bytefight.webserver.submission.domain.SubmissionValidity;
import org.bytefight.webserver.submission.infra.SubmissionRepository;
import org.bytefight.webserver.team.application.TeamService;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.domain.TeamType;
import org.bytefight.webserver.team.infra.TeamRepository;
import org.bytefight.webserver.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

class ScrimControllerIT extends FullStackIntegrationTestBase {
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private TestDataFactory testDataFactory;
  @Autowired private TeamService teamService;
  @Autowired private TeamRepository teamRepository;
  @Autowired private SubmissionRepository submissionRepository;
  @Autowired private FileRecordRepository fileRecordRepository;
  @Autowired private GameMatchRepository gameMatchRepository;
  @Autowired private TeamStatsRepository teamStatsRepository;
  @Autowired private GameMatchResultHandler resultHandler;
  @Autowired private ScrimProperties scrimProperties;
  @Autowired private RabbitTemplate rabbitTemplate;
  @Autowired private TopicExchange gameMatchExchange;
  @Autowired private org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory;

  @Test
  void schedulesUnratedScrimMatchAgainstTaBot() throws Exception {
    Fixture f = fixture("comp-scrim");
    Queue queue = bindScrimQueue(f.competition, "test.scrim.queue");

    mockMvc
        .perform(scrimPost(f, 1))
        .andExpect(status().isOk())
        // the TA bot's UUID must never leak back to the student
        .andExpect(res -> assertThat(res.getResponse().getContentAsString())
            .doesNotContain(f.taBot.getUuid().toString()))
        .andExpect(res -> assertThat(res.getResponse().getContentAsString()).contains("Yolanda"));

    Object message = rabbitTemplate.receiveAndConvert(queue.getName(), 2000);
    assertThat(message).isInstanceOf(GameMatchJob.class);
    GameMatchJob job = (GameMatchJob) message;
    assertThat(ReflectionTestUtils.getField(job, "ladder")).isEqualTo(DefaultLadders.SCRIM);
    assertThat(ReflectionTestUtils.getField(job, "reason")).isEqualTo(MatchReason.scrim);
    assertThat(ReflectionTestUtils.getField(job, "teamBUuid"))
        .isEqualTo(f.taBot.getUuid().toString());
  }

  @Test
  void dailyCapReturns429WithRetryAfter() throws Exception {
    Fixture f = fixture("comp-scrim-daily");
    bindScrimQueue(f.competition, "test.scrim.queue.daily");
    int origDaily = scrimProperties.getDailyCap();
    int origBurst = scrimProperties.getBurst();
    scrimProperties.setDailyCap(1);
    scrimProperties.setBurst(100);
    try {
      mockMvc.perform(scrimPost(f, 1)).andExpect(status().isOk());
      mockMvc
          .perform(scrimPost(f, 1))
          .andExpect(status().isTooManyRequests())
          .andExpect(header().exists("Retry-After"));

      // the cap held: exactly one scrim match was created for this competition (the DB is shared
      // across methods in this class, so scope the count to this fixture).
      long scrimMatchesForComp =
          gameMatchRepository.findAll().stream()
              .filter(m -> m.getCompetition().getId().equals(f.competition.getId()))
              .count();
      assertThat(scrimMatchesForComp).isEqualTo(1);
    } finally {
      scrimProperties.setDailyCap(origDaily);
      scrimProperties.setBurst(origBurst);
    }
  }

  @Test
  void burstCapReturns429() throws Exception {
    Fixture f = fixture("comp-scrim-burst");
    bindScrimQueue(f.competition, "test.scrim.queue.burst");
    int origBurst = scrimProperties.getBurst();
    scrimProperties.setBurst(1);
    try {
      // first leaves a scrim match in flight (waiting); nothing consumes it
      mockMvc.perform(scrimPost(f, 1)).andExpect(status().isOk());
      mockMvc
          .perform(scrimPost(f, 1))
          .andExpect(status().isTooManyRequests())
          .andExpect(header().exists("Retry-After"));
    } finally {
      scrimProperties.setBurst(origBurst);
    }
  }

  @Test
  void unknownTaBotIs404() throws Exception {
    Fixture f = fixture("comp-scrim-404");
    String body =
        objectMapper.writeValueAsString(
            Map.of("competitionSlug", f.competition.getSlug(), "taBotSlug", "nope", "count", 1));
    mockMvc
        .perform(
            post("/api/v1/scrim")
                .with(user(f.user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isNotFound());
  }

  @Test
  void regularTeamOpponentIs400() throws Exception {
    Fixture f = fixture("comp-scrim-400");
    Team regular = testDataFactory.createTeam(f.competition, UUID.randomUUID(), false);
    regular.setName("NotABot");
    setCurrentSubmission(regular);

    String body =
        objectMapper.writeValueAsString(
            Map.of(
                "competitionSlug", f.competition.getSlug(),
                "taBotSlug", "NotABot",
                "count", 1));
    mockMvc
        .perform(
            post("/api/v1/scrim")
                .with(user(f.user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void scrimResultDoesNotAffectGlicko() throws Exception {
    Fixture f = fixture("comp-scrim-unrated");
    bindScrimQueue(f.competition, "test.scrim.queue.unrated");

    mockMvc.perform(scrimPost(f, 1)).andExpect(status().isOk());
    GameMatch match =
        gameMatchRepository.findAll().stream()
            .filter(m -> m.getReason() == MatchReason.scrim)
            .findFirst()
            .orElseThrow();
    // mark it started so the result is accepted, then finalize with a win
    match.setStatus(MatchStatus.waiting);
    gameMatchRepository.save(match);

    resultHandler.handleGameMatchResult(
        new GameMatchResult(match.getUuid().toString(), MatchStatus.team_a_win));

    GameMatch after = gameMatchRepository.findByUuid(match.getUuid()).orElseThrow();
    assertThat(after.getStatus()).isEqualTo(MatchStatus.team_a_win);
    // unrated: no Glicko stats row was created for either side on the scrim ladder
    assertThat(teamStatsRepository.findByTeamAndLadder(f.student, DefaultLadders.SCRIM)).isEmpty();
    assertThat(teamStatsRepository.findByTeamAndLadder(f.taBot, DefaultLadders.SCRIM)).isEmpty();
  }

  // ---- helpers ----

  private record Fixture(Competition competition, User user, Team student, Team taBot) {}

  private Fixture fixture(String slug) {
    Competition competition = testDataFactory.createCompetition(slug, "Competition", true, 2);
    testDataFactory.createLadder(competition, DefaultLadders.SCRIM);
    User user = testDataFactory.createUser();
    Player player = testDataFactory.createPlayer(user);
    Team student = testDataFactory.createTeam(competition, UUID.randomUUID(), false);
    teamService.joinTeam(player, student);
    setCurrentSubmission(student);

    Team taBot = testDataFactory.createTeam(competition, UUID.randomUUID(), false);
    taBot.setName("Yolanda");
    taBot.setType(TeamType.ta_bot);
    setCurrentSubmission(taBot);

    return new Fixture(competition, user, student, taBot);
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder scrimPost(
      Fixture f, int count) throws Exception {
    String body =
        objectMapper.writeValueAsString(
            Map.of(
                "competitionSlug", f.competition.getSlug(),
                "taBotSlug", "Yolanda",
                "count", count));
    return post("/api/v1/scrim")
        .with(user(f.user))
        .contentType(MediaType.APPLICATION_JSON)
        .content(body);
  }

  private Queue bindScrimQueue(Competition competition, String queueName) {
    String routingKey = "competition." + competition.getSlug() + "." + DefaultLadders.SCRIM;
    Queue queue = QueueBuilder.nonDurable(queueName).autoDelete().build();
    RabbitAdmin admin = new RabbitAdmin(connectionFactory);
    admin.declareExchange(gameMatchExchange);
    admin.declareQueue(queue);
    Binding binding = BindingBuilder.bind(queue).to(gameMatchExchange).with(routingKey);
    admin.declareBinding(binding);
    return queue;
  }

  private void setCurrentSubmission(Team team) {
    FileRecord record =
        FileRecord.builder()
            .uuid(UUID.randomUUID())
            .filename("bot.zip")
            .contentType("application/zip")
            .size(1L)
            .sha256("deadbeef")
            .storagePath("/tmp/bot.zip")
            .build();
    fileRecordRepository.save(record);

    Submission submission = new Submission();
    submission.setUuid(UUID.randomUUID());
    submission.setTeam(team);
    submission.setFileRecord(record);
    submission.setValidity(SubmissionValidity.valid);
    submissionRepository.save(submission);

    team.setCurrentSubmission(submission);
    teamRepository.save(team);
  }
}
