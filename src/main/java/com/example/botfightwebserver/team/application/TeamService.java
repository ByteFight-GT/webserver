package com.example.botfightwebserver.team.application;

import com.example.botfightwebserver.auth.domain.User;
import com.example.botfightwebserver.competition.domain.Competition;
import com.example.botfightwebserver.config.ClockConfig;
import com.example.botfightwebserver.leaderboard.LeaderboardDTO;
import com.example.botfightwebserver.permissions.application.PermissionsService;
import com.example.botfightwebserver.player.domain.Player;
import com.example.botfightwebserver.player.application.PlayerService;
import com.example.botfightwebserver.player.infra.PlayerRepository;
import com.example.botfightwebserver.student.application.StudentEmailRepository;
import com.example.botfightwebserver.submission.domain.Submission;
import com.example.botfightwebserver.submission.application.SubmissionService;
import com.example.botfightwebserver.team.domain.*;
import com.example.botfightwebserver.team.domain.dto.AdminCreateTeamDto;
import com.example.botfightwebserver.team.domain.dto.PublicTeamDto;
import com.example.botfightwebserver.team.domain.dto.SelfTeamDto;
import com.example.botfightwebserver.team.domain.dto.TeamSettingsDto;
import com.example.botfightwebserver.team.infra.TeamMemberRepository;
import com.example.botfightwebserver.team.infra.TeamRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamService {
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final PlayerRepository playerRepository;
    private final SubmissionService submissionService;
    private final PlayerService playerService;

    public Team createTeam(Competition competition, TeamSettingsDto teamSettingsDto) {
        String name = teamSettingsDto.getName();

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Team name cannot be null or empty");
        }

        String normalizedName = name.trim().toLowerCase();

        if (teamRepository.existsByCompetitionAndNameNormalized(competition, normalizedName)) {
            throw new IllegalArgumentException("Team with name " + name + " already exists");
        }

        Team team = new Team();
        team.setUuid(UUID.randomUUID());
        team.setType(TeamType.regular);
        team.setCompetition(competition);
        team.setName(name);
        team.setDisplayMembers(teamSettingsDto.isDisplayMembers());

        if (teamSettingsDto.getQuote() != null) {
            team.setQuote(teamSettingsDto.getQuote());
        }

        return teamRepository.save(team);
    }

    public TeamMember joinTeam(Player player, Team team) {
        if (player == null || team == null) {
            throw new IllegalArgumentException("Player and team are required");
        }

        Competition competition = team.getCompetition();
        if (competition == null) {
            throw new IllegalArgumentException("Team must belong to a competition");
        }

        if (teamMemberRepository.existsByCompetitionAndPlayer(competition, player)) {
            throw new IllegalArgumentException("Player is already in a team for this competition");
        }

        TeamMember member = new TeamMember();
        member.setPlayer(player);
        member.setTeam(team);
        member.setCompetition(competition);
        return teamMemberRepository.save(member);
    }

    public Optional<Team> getTeamByCompetitionAndUuid(Competition competition, UUID uuid) {
        return teamRepository.findByCompetitionAndUuid(competition, uuid);
    }

    public Optional<Team> getTeamByUuid(UUID uuid) {
        return teamRepository.findByUuidAndIsDeletedFalse(uuid);
    }

    public List<Team> getTeams() {
        return teamRepository.findAll()
                .stream()
                .collect(Collectors.toUnmodifiableList());
    }

    public List<Team> getTeamsWithSubmission() {
        return teamRepository.findAllByIsDeletedFalse()
                .stream()
                .filter(team -> team.getCurrentSubmission() != null)
                .toList();
    }

    public Team getTeamById(Long id) {
        return teamRepository.getReferenceById(id);
    }

    public Optional<PublicTeamDto> getPublicTeamDtoByUuid(UUID uuid) {
        Optional<Team> team = teamRepository.findByUuidAndIsDeletedFalse(uuid);
        if (team.isEmpty()) return Optional.empty();

//        Optional<Integer> rank = teamRepository.findRankByUuid(uuid);

        return Optional.of(PublicTeamDto.from(team.get(), -1));
    }

    public Optional<SelfTeamDto> getSelfTeamDtoByUuid(UUID uuid) {
        Optional<Team> team = teamRepository.findByUuid(uuid);
        if (team.isEmpty()) return Optional.empty();

        return Optional.of(SelfTeamDto.from(team.get()));
    }

    public Integer getRankForTeam(Team team) {
        return -1;
    }

    public Team adminCreateTeam(AdminCreateTeamDto teamDto) {
        String name = teamDto.getName();
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Team name cannot be null or empty");
        }

        String normalizedName = name.trim().toLowerCase();
        if (teamRepository.existsByNameNormalized(normalizedName)) {
            throw new IllegalArgumentException("Team with name " + name + " already exists");
        }
        Team team = new Team();
        team.setName(name);
        team.setDisplayMembers(teamDto.isDisplayMembers());

        if (teamDto.getQuote() != null) {
            team.setQuote(teamDto.getQuote());
        }

        if (teamDto.getSubmissionUuid() != null) {
            Submission submission = submissionService.getSubmissionByUuid(teamDto.getSubmissionUuid());
            team.setCurrentSubmission(submission);
        }

        return teamRepository.save(team);
    }

    public void validateTeams(String team1Uuid, String team2Uuid) {
        if (team1Uuid == null || team2Uuid == null) {
            throw new IllegalArgumentException("TeamIds cannot be null");
        }
        if (!teamRepository.existsByUuid(UUID.fromString(team1Uuid)) || !teamRepository.existsByUuid(UUID.fromString(team2Uuid))) {
            throw new IllegalArgumentException("One or both teams do not exist");
        }
    }

    public Team updateAfterMatch(Team team, double glickoChange, double phiChange, double sigmaChange,
                                 boolean isWin, boolean isDraw) {
        if (isWin && isDraw) {
            throw new IllegalArgumentException("Result can't be a win and a draw");
        }
//        double currentGlicko = team.getGlicko();
//        double currentPhi = team.getPhi();
//        double currentSigma = team.getSigma();
//        double newGlicko = currentGlicko + glickoChange;
//        double newPhi = currentPhi + phiChange;
//        double newSigma = currentSigma + sigmaChange;
//        team.setGlicko(newGlicko);
//        team.setPhi(newPhi);
//        team.setSigma(newSigma);
//        team.setMatchesPlayed(team.getMatchesPlayed() + 1);
//        if (!isWin && !isDraw) {
//            team.setNumberLosses(team.getNumberLosses() + 1);
//        } else if (isWin) {
//            team.setNumberWins(team.getNumberWins() + 1);
//        } else if (isDraw) {
//            team.setNumberDraws(team.getNumberDraws() + 1);
//        }
        return teamRepository.save(team);
    }


    public void setCurrentSubmission(Long teamId, String submissionUuid) {
        if (!submissionService.isSubmissionValid(submissionUuid)) {
            throw new IllegalArgumentException("Submission is not valid");
        }
        Team team = teamRepository.findById(teamId).get();
        team.setCurrentSubmission(submissionService.getSubmissionByUuid(submissionUuid));
    }

    public Optional<Submission> getCurrentSubmission(Long teamId) {
        Optional<Submission> submission = teamRepository.findById(teamId)
                .map(Team::getCurrentSubmission);
        return submission;
    }

    public boolean setCurrentSubmissionIfNone(Long teamId, Long submissionId) {
        Team team = teamRepository.findById(teamId).get();
        if (team.getCurrentSubmission() == null) {
            team.setCurrentSubmission(submissionService.getSubmissionReferenceById(submissionId));
            return true;
        }
        return false;
    }

    public boolean isExistById(Long teamId) {
        return teamRepository.existsById(teamId);
    }


    public List<Team> pagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page index must be zero or greater");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Page size must be greater than 0");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "glicko"));

        Page<Team> teamPage = teamRepository.findAll(pageable);

        // Return the teams as a list
        return teamPage.getContent();
    }

    @Transactional
    public void editTeam(Long teamId, TeamSettingsDto teamSettingsDto) {
        if (!teamRepository.existsById(teamId)) {
            throw new IllegalArgumentException("Team with id " + teamId + " does not exist");
        }
        Team team = teamRepository.findById(teamId).get();

        if (team.isDeleted()) {
            throw new IllegalArgumentException("This team is deleted and cannot be edited.");
        }

        if (teamSettingsDto.getName() != null) team.setName(teamSettingsDto.getName());
        if (teamSettingsDto.getQuote() != null) team.setQuote(teamSettingsDto.getQuote());
        team.setDisplayMembers(teamSettingsDto.isDisplayMembers());

        teamRepository.save(team);
    }

    @Transactional
    public void deleteTeam(Long teamId, TeamDeletionReason reason) {
        Team team = teamRepository.findById(teamId).orElseThrow();

        if (team.isDeleted()) {
            throw new IllegalArgumentException("This team is already deleted.");
        }

        // remove all players from the team
        List<Player> players = playerService.getPlayersByTeam(teamId);
        for (Player p : players) {
            playerService.leaveTeam(p);
        }

        // mark the team as deleted
//        team.setDeleted(true);
//        team.setDeletedAt(LocalDateTime.now());

        teamRepository.save(team);
    }

    public boolean isTeamJoinable(Team team) {
        return !team.isDeleted();
    }

    public List<LeaderboardDTO> getLeaderboard() {
        return getLeaderboard(0, Integer.MAX_VALUE).toList();
    }

    public Page<LeaderboardDTO> getLeaderboard(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        AtomicInteger rank = new AtomicInteger(1 + page * size);
        Page<Team> teamPage = null;

        List<UUID> teamUuids = teamPage.map(Team::getUuid).stream().toList();
        List<Player> teamPlayers = playerRepository.findMembersByTeamUuids(teamUuids);

        Map<UUID, List<String>> membersByTeamUuid = teamPlayers.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getTeam().getUuid(),
                        Collectors.mapping(Player::getUsername, Collectors.toList())
                ));

        return teamPage.map(team -> teamToLeaderboardDTO(team, rank.getAndIncrement(), membersByTeamUuid.get(team.getUuid())));
    }

    private LeaderboardDTO teamToLeaderboardDTO(Team team, int rank, List<String> memberNames) {
        LeaderboardDTO.LeaderboardDTOBuilder builder = LeaderboardDTO.builder();
        builder.teamUuid(team.getUuid().toString())
                .rank(rank)
//                .glicko(team.getCurrentSubmission() != null ? team.getGlicko() : -1)
                .teamName(team.getName())
//                .createdAt(team.getCreationDateTime())
                .type(team.getType())
                .quote(team.getQuote());

        if (team.isDisplayMembers()) {
            builder.members(memberNames);
        }

        return builder.build();
    }

    public Team findTeamByCode(String code) {
        Optional<Team> team = teamRepository.findByJoinCode(code);
        return team.orElseThrow(() -> new IllegalArgumentException("Team with code " + code + " does not exist"));
    }

    public int countTeamsWithSubmission() {
        return teamRepository.countByCurrentSubmissionNotNull();
    }

    public boolean isNameExist(String name) {
        if (name == null) {
            return false;
        }
        return teamRepository.existsByNameNormalized(name.trim().toLowerCase());
    }
}
