package com.example.botfightwebserver.team.application;

import com.example.botfightwebserver.auth.domain.User;
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
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final SubmissionService submissionService;
    private final PlayerService playerService;
    private final ClockConfig clockConfig;
    private static final int MAX_PLAYERS = 2;
    private final PermissionsService permissionsService;
    private final StudentEmailRepository studentEmailRepository;

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

    public Optional<Team> getTeamByUuid(String uuid) {
        return teamRepository.findByUuid(UUID.fromString(uuid));
    }

    public Optional<PublicTeamDto> getPublicTeamDtoByUuid(String uuid) {
        Optional<Team> team = teamRepository.findByUuid(UUID.fromString(uuid));
        if (team.isEmpty()) return Optional.empty();

        Optional<Integer> rank = teamRepository.findRankByUuid(UUID.fromString(uuid));

        return Optional.of(PublicTeamDto.from(team.get(), rank.orElse(null)));
    }

    public Optional<SelfTeamDto> getSelfTeamDtoByUuid(String uuid) {
        Optional<Team> team = teamRepository.findByUuid(UUID.fromString(uuid));
        if (team.isEmpty()) return Optional.empty();

        Optional<Integer> rank = teamRepository.findRankByUuid(UUID.fromString(uuid));

        return Optional.of(SelfTeamDto.from(team.get(), rank.orElse(null)));
    }

    public Integer getRankForTeam(Team team) {
        return teamRepository.findRankByUuid(team.getUuid()).orElse(null);
    }

    public Team createTeam(User user, TeamSettingsDto teamSettingsDto) {
        String name = teamSettingsDto.getName();

        if(permissionsService.get().getRestrictTeamCreationToStudentEmails()) {
            if(!studentEmailRepository.existsByEmail(user.getEmail())) {
                throw new IllegalArgumentException("You are not whitelisted for this competition.");
            }
        }

        if (teamRepository.existsByName(name.trim())) {
            throw new IllegalArgumentException("Team with name " + name + " already exists");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Team name cannot be null or empty");
        }
        Team team = new Team();
        team.setName(name);
        team.setDisplayMembers(teamSettingsDto.isDisplayMembers());

        if (teamSettingsDto.getQuote() != null) {
            team.setQuote(teamSettingsDto.getQuote());
        }

        return teamRepository.save(team);
    }

    public Team adminCreateTeam(AdminCreateTeamDto teamDto) {
        String name = teamDto.getName();
        if (teamRepository.existsByName(name.trim())) {
            throw new IllegalArgumentException("Team with name " + name + " already exists");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Team name cannot be null or empty");
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

        if(team.isDeleted()) {
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

        if(team.isDeleted()) {
            throw new IllegalArgumentException("This team is already deleted.");
        }

        // remove all players from the team
        List<Player> players = playerService.getPlayersByTeam(teamId);
        for(Player p : players) {
            playerService.leaveTeam(p);
        }

        // mark the team as deleted
        team.setDeleted(true);
        team.setDeletedAt(LocalDateTime.now());

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
        Page<Team> teamPage = teamRepository.findTeamsPaginated(pageable);

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
        Optional<Team> team = teamRepository.findByTeamCode(code);
        return team.orElseThrow(() -> new IllegalArgumentException("Team with code " + code + " does not exist"));
    }

    public int countTeamsWithSubmission() {
        return teamRepository.countByCurrentSubmissionNotNull();
    }

    public boolean isNameExist(String name) {
        return teamRepository.existsByName(name);
    }
}


