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
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamService {
    private static final String JOIN_CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int JOIN_CODE_LENGTH = 8;
    private static final int JOIN_CODE_MAX_ATTEMPTS = 10;

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final PlayerRepository playerRepository;
    private final SubmissionService submissionService;
    private final PlayerService playerService;

    public String generateJoinCode() {
        for (int attempt = 0; attempt < JOIN_CODE_MAX_ATTEMPTS; attempt++) {
            String code = generateJoinCodeCandidate();
            if (!teamRepository.existsByJoinCode(code)) {
                return code;
            }
        }

        throw new IllegalStateException("Failed to generate unique join code after " + JOIN_CODE_MAX_ATTEMPTS + " attempts");
    }

    private String generateJoinCodeCandidate() {
        StringBuilder builder = new StringBuilder(JOIN_CODE_LENGTH);
        for (int i = 0; i < JOIN_CODE_LENGTH; i++) {
            int index = ThreadLocalRandom.current().nextInt(JOIN_CODE_ALPHABET.length());
            builder.append(JOIN_CODE_ALPHABET.charAt(index));
        }
        return builder.toString();
    }

    public Team createTeam(Competition competition, TeamSettingsDto teamSettingsDto) {
        if(!competition.isActive()) {
            throw new IllegalArgumentException("Competition is not active");
        }

        String name = teamSettingsDto.getName();

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Team name cannot be null or empty");
        }

        String normalizedName = name.trim().toLowerCase();

        if (teamRepository.existsByCompetitionAndNameNormalized(competition, normalizedName)) {
            throw new IllegalArgumentException("Team with name " + name + " already exists");
        }

        String joinCode = generateJoinCode();

        Team team = new Team();
        team.setUuid(UUID.randomUUID());
        team.setType(TeamType.regular);
        team.setCompetition(competition);
        team.setName(name);
        team.setDisplayMembers(teamSettingsDto.getDisplayMembers());
        team.setJoinCode(joinCode);

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

        if(!competition.isActive()) {
            throw new IllegalArgumentException("Competition is not active");
        }

        if (teamMemberRepository.existsByCompetitionAndPlayer(competition, player)) {
            throw new IllegalArgumentException("Player is already in a team for this competition");
        }

        if(countPlayersForTeam(team) >= competition.getMaxPlayersPerTeam()) {
            throw new IllegalArgumentException("Team is full");
        }

        TeamMember member = new TeamMember();
        member.setPlayer(player);
        member.setTeam(team);
        member.setCompetition(competition);
        return teamMemberRepository.save(member);
    }

    public void leaveTeam(Competition competition, Player player) {
        if (player == null || competition == null) {
            throw new IllegalArgumentException("Competition and player are required");
        }

        if(!competition.isActive()) {
            throw new IllegalArgumentException("Competition is not active");
        }

        TeamMember member = teamMemberRepository.findByCompetitionAndPlayer(competition, player)
                .orElseThrow(() -> new IllegalArgumentException("Player is not in a team for this competition"));

        Team team = member.getTeam();

        teamMemberRepository.delete(member);

        if(countPlayersForTeam(team) == 0) {
            team.softDelete();
            teamRepository.save(team);
        }
    }

    public TeamMember joinTeamByJoinCode(Competition competition, Player player, String joinCode) {
        Team team = teamRepository.findByCompetitionAndJoinCodeAndIsDeletedIsFalse(competition, joinCode).orElseThrow(() -> new IllegalArgumentException("A team with that join code was no found"));
        return joinTeam(player, team);
    }

    public Optional<Team> getTeamByUuid(UUID uuid) {
        return teamRepository.findByUuidAndIsDeletedFalse(uuid);
    }

    public List<Player> getPlayersForTeam(Team team) {
        if (team == null || team.getId() == null) {
            throw new IllegalArgumentException("Team is required");
        }

        return teamMemberRepository.findByTeam(team)
                .stream()
                .map(TeamMember::getPlayer)
                .toList();
    }

    public boolean isMember(Team team, Player player) {
        if (team == null || player == null) {
            throw new IllegalArgumentException("Team and player are required");
        }

        return teamMemberRepository.existsByTeamAndPlayer(team, player);
    }

    public long countPlayersForTeam(Team team) {
        if (team == null || team.getId() == null) {
            throw new IllegalArgumentException("Team is required");
        }

        return teamMemberRepository.countByTeam(team);
    }

    public Optional<Team> getTeamByCompetitionAndUuid(Competition competition, UUID uuid) {
        return teamRepository.findByCompetitionAndUuid(competition, uuid);
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

    public Integer getRankForTeam(Team team) {
        return -1;
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
        editTeam(team, teamSettingsDto);
    }

    @Transactional
    public void editTeam(Team team, TeamSettingsDto teamSettingsDto) {
        if (team == null) {
            throw new IllegalArgumentException("Team is required");
        }

        if (team.isDeleted()) {
            throw new IllegalArgumentException("This team is deleted and cannot be edited.");
        }

        if (teamSettingsDto.getName() != null) {
            String normalizedName = teamSettingsDto.getName().trim().toLowerCase();
            if (!normalizedName.equals(team.getNameNormalized())
                    && teamRepository.existsByCompetitionAndNameNormalized(team.getCompetition(), normalizedName)) {
                throw new IllegalArgumentException("Team with name " + teamSettingsDto.getName() + " already exists");
            }
            team.setName(teamSettingsDto.getName());
        }
        if (teamSettingsDto.getQuote() != null) team.setQuote(teamSettingsDto.getQuote());
        if(teamSettingsDto.getDisplayMembers() != null) team.setDisplayMembers(teamSettingsDto.getDisplayMembers());

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
}
