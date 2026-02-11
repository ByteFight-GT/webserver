package org.bytefight.webserver.team;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.TestDataFactory;
import org.bytefight.webserver.auth.domain.User;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.storage.domain.FileRecord;
import org.bytefight.webserver.storage.infra.FileRecordRepository;
import org.bytefight.webserver.submission.domain.Submission;
import org.bytefight.webserver.submission.domain.SubmissionValidity;
import org.bytefight.webserver.submission.infra.SubmissionRepository;
import org.bytefight.webserver.team.application.TeamService;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.domain.dto.SetSubmissionDto;
import org.bytefight.webserver.team.domain.dto.TeamSettingsDto;
import org.bytefight.webserver.team.infra.TeamRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class PrivateTeamControllerIT extends FullStackIntegrationTestBase {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataFactory testDataFactory;

    @Autowired
    private TeamService teamService;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private FileRecordRepository fileRecordRepository;

    @Test
    void editTeamUpdatesTeamSettings() throws Exception {
        Competition competition = testDataFactory.createCompetition();
        Team team = testDataFactory.createTeam(competition);
        Player player = testDataFactory.createUserWithPlayer();
        User user = player.getUser();
        teamService.joinTeam(player, team);

        TeamSettingsDto dto = TeamSettingsDto.builder()
                .name("Updated Name")
                .quote("New quote")
                .displayMembers(true)
                .build();

        mockMvc.perform(post("/api/v1/team/{uuid}", team.getUuid())
                        .with(user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.quote").value("New quote"))
                .andExpect(jsonPath("$.displayMembers").value(true));

        Team updated = teamRepository.findByUuid(team.getUuid()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Updated Name");
        assertThat(updated.getQuote()).isEqualTo("New quote");
        assertThat(updated.isDisplayMembers()).isTrue();
    }

    @Test
    void editTeamRejectsNonMember() throws Exception {
        Competition competition = testDataFactory.createCompetition();
        Team team = testDataFactory.createTeam(competition);
        Player player = testDataFactory.createUserWithPlayer();
        User user = player.getUser();

        TeamSettingsDto dto = TeamSettingsDto.builder()
                .name("Updated Name")
                .displayMembers(true)
                .build();

        mockMvc.perform(post("/api/v1/team/{uuid}", team.getUuid())
                        .with(user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void editTeamRejectsDuplicateName() throws Exception {
        Competition competition = testDataFactory.createCompetition();
        Team team = testDataFactory.createTeam(competition);
        Team otherTeam = testDataFactory.createTeam(competition);
        otherTeam.setName("Taken Name");
        teamRepository.save(otherTeam);

        Player player = testDataFactory.createUserWithPlayer();
        User user = player.getUser();
        teamService.joinTeam(player, team);

        TeamSettingsDto dto = TeamSettingsDto.builder()
                .name("taken name")
                .displayMembers(true)
                .build();

        mockMvc.perform(post("/api/v1/team/{uuid}", team.getUuid())
                        .with(user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void editTeamNotFound() throws Exception {
        Player player = testDataFactory.createUserWithPlayer();
        User user = player.getUser();

        TeamSettingsDto dto = TeamSettingsDto.builder()
                .name("Updated Name")
                .displayMembers(true)
                .build();

        mockMvc.perform(post("/api/v1/team/{uuid}", UUID.randomUUID())
                        .with(user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void setCurrentSubmissionUpdatesTeamSubmission() throws Exception {
        Competition competition = testDataFactory.createCompetition();
        Team team = testDataFactory.createTeam(competition);
        Player player = testDataFactory.createUserWithPlayer();
        User user = player.getUser();
        teamService.joinTeam(player, team);

        Submission submission = createSubmission(team, SubmissionValidity.valid);
        SetSubmissionDto dto = new SetSubmissionDto(getSubmissionUuid(submission).toString());

        mockMvc.perform(patch("/api/v1/team/{uuid}/current-submission", getTeamUuid(team))
                        .with(user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        Team updated = teamRepository.findByUuid(getTeamUuid(team)).orElseThrow();
        Submission currentSubmission = (Submission) ReflectionTestUtils.getField(updated, "currentSubmission");
        assertThat(currentSubmission).isNotNull();
        assertThat(getSubmissionUuid(currentSubmission)).isEqualTo(getSubmissionUuid(submission));
    }

    @Test
    void setCurrentSubmissionRejectsNonMember() throws Exception {
        Competition competition = testDataFactory.createCompetition();
        Team team = testDataFactory.createTeam(competition);
        Player player = testDataFactory.createUserWithPlayer();
        User user = player.getUser();

        Submission submission = createSubmission(team, SubmissionValidity.valid);
        SetSubmissionDto dto = new SetSubmissionDto(getSubmissionUuid(submission).toString());

        mockMvc.perform(patch("/api/v1/team/{uuid}/current-submission", getTeamUuid(team))
                        .with(user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void setCurrentSubmissionRejectsInvalidSubmission() throws Exception {
        Competition competition = testDataFactory.createCompetition();
        Team team = testDataFactory.createTeam(competition);
        Player player = testDataFactory.createUserWithPlayer();
        User user = player.getUser();
        teamService.joinTeam(player, team);

        Submission submission = createSubmission(team, SubmissionValidity.invalid);
        SetSubmissionDto dto = new SetSubmissionDto(getSubmissionUuid(submission).toString());

        mockMvc.perform(patch("/api/v1/team/{uuid}/current-submission", getTeamUuid(team))
                        .with(user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());

        Team updated = teamRepository.findByUuid(getTeamUuid(team)).orElseThrow();
        Submission currentSubmission = (Submission) ReflectionTestUtils.getField(updated, "currentSubmission");
        assertThat(currentSubmission).isNull();
    }

    @Test
    void setCurrentSubmissionRejectsOtherTeamSubmission() throws Exception {
        Competition competition = testDataFactory.createCompetition();
        Team team = testDataFactory.createTeam(competition);
        Team otherTeam = testDataFactory.createTeam(competition);
        Player player = testDataFactory.createUserWithPlayer();
        User user = player.getUser();
        teamService.joinTeam(player, team);

        Submission submission = createSubmission(otherTeam, SubmissionValidity.valid);
        SetSubmissionDto dto = new SetSubmissionDto(getSubmissionUuid(submission).toString());

        mockMvc.perform(patch("/api/v1/team/{uuid}/current-submission", getTeamUuid(team))
                        .with(user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());

        Team updated = teamRepository.findByUuid(getTeamUuid(team)).orElseThrow();
        Submission currentSubmission = (Submission) ReflectionTestUtils.getField(updated, "currentSubmission");
        assertThat(currentSubmission).isNull();
    }

    @Test
    void setCurrentSubmissionNotFoundWhenSubmissionMissing() throws Exception {
        Competition competition = testDataFactory.createCompetition();
        Team team = testDataFactory.createTeam(competition);
        Player player = testDataFactory.createUserWithPlayer();
        User user = player.getUser();
        teamService.joinTeam(player, team);

        SetSubmissionDto dto = new SetSubmissionDto(UUID.randomUUID().toString());

        mockMvc.perform(patch("/api/v1/team/{uuid}/current-submission", getTeamUuid(team))
                        .with(user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    private Submission createSubmission(Team team, SubmissionValidity validity) {
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
        ReflectionTestUtils.setField(submission, "validity", validity);
        return submissionRepository.save(submission);
    }

    private UUID getTeamUuid(Team team) {
        return (UUID) ReflectionTestUtils.getField(team, "uuid");
    }

    private UUID getSubmissionUuid(Submission submission) {
        return (UUID) ReflectionTestUtils.getField(submission, "uuid");
    }
}
