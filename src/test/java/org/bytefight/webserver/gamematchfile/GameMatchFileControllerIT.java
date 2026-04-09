package org.bytefight.webserver.gamematchfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.transaction.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.TestDataFactory;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.gamematch.application.GameMatchService;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchReason;
import org.bytefight.webserver.gamematchfile.domain.GameMatchFileVisibility;
import org.bytefight.webserver.gamematchfile.infra.GameMatchFileRepository;
import org.bytefight.webserver.storage.domain.FileRecord;
import org.bytefight.webserver.storage.infra.FileRecordRepository;
import org.bytefight.webserver.submission.domain.Submission;
import org.bytefight.webserver.submission.infra.SubmissionRepository;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.user.domain.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@Transactional
class GameMatchFileControllerIT extends FullStackIntegrationTestBase {
  private static Path storageRoot;

  @Autowired private MockMvc mockMvc;

  @Autowired private TestDataFactory testDataFactory;

  @Autowired private GameMatchService gameMatchService;

  @Autowired private GameMatchFileRepository gameMatchFileRepository;

  @Autowired private SubmissionRepository submissionRepository;

  @Autowired private FileRecordRepository fileRecordRepository;

  @BeforeAll
  static void setupStorageRoot() throws IOException {
    storageRoot = Files.createTempDirectory("match-file-storage-test-");
  }

  @DynamicPropertySource
  static void configureStorageProperties(DynamicPropertyRegistry registry) {
    registry.add("storage.root", () -> storageRoot.toString());
    registry.add("storage.hmac-secret", () -> "test-secret");
  }

  @BeforeEach
  void clearStorage() throws IOException {
    gameMatchFileRepository.deleteAll();
    submissionRepository.deleteAll();
    fileRecordRepository.deleteAll();
    if (Files.exists(storageRoot)) {
      try (Stream<Path> paths = Files.walk(storageRoot)) {
        paths
            .sorted(Comparator.reverseOrder())
            .filter(path -> !path.equals(storageRoot))
            .forEach(
                path -> {
                  try {
                    Files.deleteIfExists(path);
                  } catch (IOException ignored) {
                  }
                });
      }
    }
  }

  @Test
  void uploadGameMatchFileCreatesRecordForAdmin() throws Exception {
    Competition competition = testDataFactory.createCompetition("comp", "Competition", true, 2);
    testDataFactory.createLadder(competition, "ladder");
    Team teamA = testDataFactory.createTeam(competition, UUID.randomUUID(), false);
    Team teamB = testDataFactory.createTeam(competition, UUID.randomUUID(), false);
    User user = testDataFactory.createUser();

    Submission submissionA = createSubmission(teamA);
    Submission submissionB = createSubmission(teamB);

    GameMatch match =
        gameMatchService.createMatch(
            user,
            teamA,
            teamB,
            submissionA,
            submissionB,
            "ladder",
            MatchReason.matchmaking,
            null,
            null);
    UUID matchUuid = (UUID) ReflectionTestUtils.getField(match, "uuid");
    UUID teamAUuid = (UUID) ReflectionTestUtils.getField(teamA, "uuid");

    MockMultipartFile file =
        new MockMultipartFile("file", "results.json", "application/json", "{}".getBytes());

    mockMvc
        .perform(
            multipart("/api/v1/game-match-file")
                .file(file)
                .param("gameMatchUuid", matchUuid.toString())
                .param("teamUuid", teamAUuid.toString())
                .param("slug", "match-log")
                .param("visibility", GameMatchFileVisibility.everyone.name())
                .with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk());

    assertThat(gameMatchFileRepository.count()).isEqualTo(1);
    var stored = gameMatchFileRepository.findAll().get(0);
    String storedSlug = (String) ReflectionTestUtils.getField(stored, "slug");
    Object storedMatch = ReflectionTestUtils.getField(stored, "gameMatch");
    Object storedTeam = ReflectionTestUtils.getField(stored, "team");
    assertThat(storedSlug).isEqualTo("match-log");
    assertThat(storedMatch).isNotNull();
    assertThat(storedTeam).isNotNull();
  }

  @Test
  void uploadGameMatchFileForbiddenForNonAdmin() throws Exception {
    Competition competition = testDataFactory.createCompetition("comp", "Competition", true, 2);
    testDataFactory.createLadder(competition, "ladder");
    Team teamA = testDataFactory.createTeam(competition, UUID.randomUUID(), false);
    Team teamB = testDataFactory.createTeam(competition, UUID.randomUUID(), false);
    User user = testDataFactory.createUser();

    Submission submissionA = createSubmission(teamA);
    Submission submissionB = createSubmission(teamB);

    GameMatch match =
        gameMatchService.createMatch(
            user,
            teamA,
            teamB,
            submissionA,
            submissionB,
            "ladder",
            MatchReason.matchmaking,
            null,
            null);
    UUID matchUuid = (UUID) ReflectionTestUtils.getField(match, "uuid");

    MockMultipartFile file =
        new MockMultipartFile("file", "results.json", "application/json", "{}".getBytes());

    mockMvc
        .perform(
            multipart("/api/v1/game-match-file")
                .file(file)
                .param("gameMatchUuid", matchUuid.toString())
                .param("slug", "match-log")
                .param("visibility", GameMatchFileVisibility.everyone.name())
                .with(user("user").roles("USER")))
        .andExpect(status().isForbidden());
  }

  private Submission createSubmission(Team team) {
    FileRecord record = new FileRecord();
    ReflectionTestUtils.setField(record, "uuid", UUID.randomUUID());
    ReflectionTestUtils.setField(record, "filename", "bot.zip");
    ReflectionTestUtils.setField(record, "contentType", "application/zip");
    ReflectionTestUtils.setField(record, "size", 1L);
    ReflectionTestUtils.setField(record, "sha256", "deadbeef");
    ReflectionTestUtils.setField(record, "storagePath", "/tmp/bot.zip");
    fileRecordRepository.save(record);

    Submission submission = new Submission();
    ReflectionTestUtils.setField(submission, "uuid", UUID.randomUUID());
    ReflectionTestUtils.setField(submission, "team", team);
    ReflectionTestUtils.setField(submission, "fileRecord", record);
    return submissionRepository.save(submission);
  }
}
