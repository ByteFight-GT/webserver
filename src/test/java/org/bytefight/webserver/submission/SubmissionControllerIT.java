package org.bytefight.webserver.submission;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.TestDataFactory;
import org.bytefight.webserver.auth.domain.User;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.gamematch.domain.DefaultLadders;
import org.bytefight.webserver.gamematch.domain.dto.GameMatchJob;
import org.bytefight.webserver.storage.infra.FileRecordRepository;
import org.bytefight.webserver.submission.infra.SubmissionRepository;
import org.bytefight.webserver.team.application.TeamService;
import org.bytefight.webserver.team.domain.Team;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SubmissionControllerIT extends FullStackIntegrationTestBase {
    private static Path storageRoot;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestDataFactory testDataFactory;

    @Autowired
    private TeamService teamService;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private FileRecordRepository fileRecordRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TopicExchange gameMatchExchange;

    @Autowired
    private org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory;

    @BeforeAll
    static void setupStorageRoot() throws IOException {
        storageRoot = Files.createTempDirectory("submission-storage-test-");
    }

    @DynamicPropertySource
    static void configureStorageProperties(DynamicPropertyRegistry registry) {
        registry.add("storage.root", () -> storageRoot.toString());
        registry.add("storage.hmac-secret", () -> "test-secret");
    }

    @BeforeEach
    void clearState() throws IOException {
        submissionRepository.deleteAll();
        fileRecordRepository.deleteAll();
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
    void uploadSubmissionCreatesSubmissionAndQueuesValidationMatch() throws Exception {
        Competition competition = testDataFactory.createCompetition("comp", "Competition", true, 2);
        Team team = testDataFactory.createTeam(competition);
        User user = testDataFactory.createUser();
        var player = testDataFactory.createPlayer(user);
        teamService.joinTeam(player, team);

        String routingKey = "competition." + competition.getSlug() + "." + DefaultLadders.VALIDATION;
        Queue queue = QueueBuilder.nonDurable("test.submission.match.queue").autoDelete().build();
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.declareExchange(gameMatchExchange);
        admin.declareQueue(queue);
        Binding binding = BindingBuilder.bind(queue).to(gameMatchExchange).with(routingKey);
        admin.declareBinding(binding);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "bot.zip",
                "application/zip",
                "payload".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/submission/team/{teamUuid}", team.getUuid())
                        .file(file)
                        .param("description", "test")
                        .param("isAutoSet", "false")
                        .with(user(user)))
                .andExpect(status().isOk());

        assertThat(submissionRepository.count()).isEqualTo(1);

        Object message = rabbitTemplate.receiveAndConvert(queue.getName(), 2000);
        assertThat(message).isInstanceOf(GameMatchJob.class);
        GameMatchJob job = (GameMatchJob) message;
        assertThat(job.getCompetitionSlug()).isEqualTo(competition.getSlug());
        assertThat(job.getLadder()).isEqualTo(DefaultLadders.VALIDATION);
    }
}
