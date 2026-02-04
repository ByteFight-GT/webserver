package org.bytefight.webserver.gamematchfile;

import jakarta.transaction.Transactional;
import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.TestDataFactory;
import org.bytefight.webserver.auth.domain.User;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.gamematch.application.GameMatchService;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchReason;
import org.bytefight.webserver.gamematchfile.domain.GameMatchFileVisibility;
import org.bytefight.webserver.gamematchfile.infra.GameMatchFileRepository;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.player.infra.PlayerRepository;
import org.bytefight.webserver.storage.domain.FileRecord;
import org.bytefight.webserver.storage.infra.FileRecordRepository;
import org.bytefight.webserver.submission.domain.Submission;
import org.bytefight.webserver.submission.infra.SubmissionRepository;
import org.bytefight.webserver.team.application.TeamService;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.infra.TeamMemberRepository;
import org.bytefight.webserver.team.infra.TeamRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class GameMatchFileDownloadIT extends FullStackIntegrationTestBase {
    private static Path storageRoot;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestDataFactory testDataFactory;

    @Autowired
    private GameMatchService gameMatchService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private GameMatchFileRepository gameMatchFileRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private FileRecordRepository fileRecordRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @BeforeAll
    static void setupStorageRoot() throws IOException {
        storageRoot = Files.createTempDirectory("match-file-download-test-");
    }

    @DynamicPropertySource
    static void configureStorageProperties(DynamicPropertyRegistry registry) {
        registry.add("storage.root", () -> storageRoot.toString());
        registry.add("storage.hmac-secret", () -> "test-secret");
    }

    @BeforeEach
    void clearState() throws IOException {
        gameMatchFileRepository.deleteAll();
        submissionRepository.deleteAll();
        fileRecordRepository.deleteAll();
        teamMemberRepository.deleteAll();
        teamRepository.deleteAll();
        playerRepository.deleteAll();
        if (Files.exists(storageRoot)) {
            try (Stream<Path> paths = Files.walk(storageRoot)) {
                paths.sorted(Comparator.reverseOrder())
                        .filter(path -> !path.equals(storageRoot))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException ignored) {
                            }
                        });
            }
        }
    }

    @Test
    void downloadCommonFileRespectsVisibility() throws Exception {
        MatchSetup setup = createMatchSetup();

        uploadMatchFile(setup.match, null, "public-log", GameMatchFileVisibility.everyone);
        uploadMatchFile(setup.match, null, "admin-log", GameMatchFileVisibility.admin);

        UUID matchUuid = getUuid(setup.match);
        UUID teamAUuid = getUuid(setup.teamA);
        UUID teamBUuid = getUuid(setup.teamB);

        mockMvc.perform(get("/api/v1/game-match-file/{matchUuid}/{slug}", matchUuid, "public-log")
                        .with(user(setup.viewer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.downloadLink.uri").exists());

        mockMvc.perform(get("/api/v1/game-match-file/{matchUuid}/{slug}", matchUuid, "admin-log")
                        .with(user(setup.admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.downloadLink.uri").exists());

        mockMvc.perform(get("/api/v1/game-match-file/{matchUuid}/{slug}", matchUuid, "admin-log")
                        .with(user(setup.viewer)))
                .andExpect(status().isForbidden());
    }

    @Test
    void downloadTeamFilesAllowsSameSlugDifferentTeams() throws Exception {
        MatchSetup setup = createMatchSetup();
        String slug = "match-log";
        UUID matchUuid = getUuid(setup.match);
        UUID teamAUuid = getUuid(setup.teamA);
        UUID teamBUuid = getUuid(setup.teamB);

        uploadMatchFile(setup.match, setup.teamA, slug, GameMatchFileVisibility.team);
        uploadMatchFile(setup.match, setup.teamB, slug, GameMatchFileVisibility.team);

        mockMvc.perform(get("/api/v1/game-match-file/{matchUuid}/{slug}/team/{teamUuid}",
                        matchUuid, slug, teamAUuid)
                        .with(user(setup.teamAUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamUuid").value(teamAUuid.toString()));

        mockMvc.perform(get("/api/v1/game-match-file/{matchUuid}/{slug}/team/{teamUuid}",
                        matchUuid, slug, teamBUuid)
                        .with(user(setup.teamBUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamUuid").value(teamBUuid.toString()));

        mockMvc.perform(get("/api/v1/game-match-file/{matchUuid}/{slug}/team/{teamUuid}",
                        matchUuid, slug, teamAUuid)
                        .with(user(setup.viewer)))
                .andExpect(status().isForbidden());

        long sameSlugCount = gameMatchFileRepository.findAll().stream()
                .filter(file -> slug.equals(getSlug(file)))
                .count();
        assertThat(sameSlugCount).isEqualTo(2);
    }

    private void uploadMatchFile(GameMatch match, Team team, String slug, GameMatchFileVisibility visibility) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                slug + ".json",
                "application/json",
                "{}".getBytes()
        );

        MockHttpServletRequestBuilder request = multipart("/api/v1/game-match-file")
                .file(file)
                .param("gameMatchUuid", getUuid(match).toString())
                .param("slug", slug)
                .param("visibility", visibility.name())
                .with(user("admin").roles("ADMIN"));

        if (team != null) {
            request = request.param("teamUuid", getUuid(team).toString());
        }

        mockMvc.perform(request)
                .andExpect(status().isOk());
    }

    private MatchSetup createMatchSetup() {
        Competition competition = testDataFactory.createCompetition("comp", "Competition", true, 4);
        testDataFactory.createLadder(competition, "ladder");
        Team teamA = testDataFactory.createTeam(competition, UUID.randomUUID(), false);
        Team teamB = testDataFactory.createTeam(competition, UUID.randomUUID(), false);
        User admin = testDataFactory.createUser("admin@example.com", true);
        User viewer = testDataFactory.createUser("viewer@example.com", false);
        Player teamAPlayer = testDataFactory.createPlayer(testDataFactory.createUser("team-a@example.com", false));
        Player teamBPlayer = testDataFactory.createPlayer(testDataFactory.createUser("team-b@example.com", false));

        teamService.joinTeam(teamAPlayer, teamA);
        teamService.joinTeam(teamBPlayer, teamB);

        Submission submissionA = createSubmission(teamA);
        Submission submissionB = createSubmission(teamB);

        GameMatch match = gameMatchService.createMatch(
                admin,
                teamA,
                teamB,
                submissionA,
                submissionB,
                "ladder",
                MatchReason.ladder,
                null
        );

        User teamAUser = (User) ReflectionTestUtils.getField(teamAPlayer, "user");
        User teamBUser = (User) ReflectionTestUtils.getField(teamBPlayer, "user");

        return new MatchSetup(match, teamA, teamB, admin, viewer, teamAUser, teamBUser);
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

    private UUID getUuid(Object entity) {
        return (UUID) ReflectionTestUtils.getField(entity, "uuid");
    }

    private String getSlug(Object entity) {
        return (String) ReflectionTestUtils.getField(entity, "slug");
    }

    private record MatchSetup(
            GameMatch match,
            Team teamA,
            Team teamB,
            User admin,
            User viewer,
            User teamAUser,
            User teamBUser
    ) {
    }
}
