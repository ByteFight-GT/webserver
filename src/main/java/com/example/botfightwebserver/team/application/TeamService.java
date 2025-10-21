package com.example.botfightwebserver.team.application;

import com.example.botfightwebserver.config.ClockConfig;
import com.example.botfightwebserver.leaderboard.LeaderboardDTO;
import com.example.botfightwebserver.player.domain.Player;
import com.example.botfightwebserver.player.application.PlayerService;
import com.example.botfightwebserver.submission.Submission;
import com.example.botfightwebserver.submission.SubmissionService;
import com.example.botfightwebserver.team.domain.EditTeamDto;
import com.example.botfightwebserver.team.infra.TeamRepository;
import com.example.botfightwebserver.team.domain.Team;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamService {

    private final TeamRepository teamRepository;
    private final SubmissionService submissionService;
    private final PlayerService playerService;
    private final ClockConfig clockConfig;
    private static final int MAX_PLAYERS = 2;

    public List<Team> getTeams() {
        return teamRepository.findAll()
                .stream()
                .collect(Collectors.toUnmodifiableList());
    }

    public List<Team> getTeamsWithSubmission() {
        return teamRepository.findAll()
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

    public Team createTeam(String name) {
        if (teamRepository.existsByName(name.trim())) {
            throw new IllegalArgumentException("Team with name " + name + " already exists");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Team name cannot be null or empty");
        }
        Team team = new Team();
        team.setName(name);
        return teamRepository.save(team);
    }

    public void validateTeams(Long team1Id, Long team2Id) {
        if (team1Id == null || team2Id == null) {
            throw new IllegalArgumentException("TeamIds cannot be null");
        }
        if (!teamRepository.existsById(team1Id) || !teamRepository.existsById(team2Id)) {
            throw new IllegalArgumentException("One or both teams do not exist");
        }
    }

    public Team updateAfterMatch(Team team, double glickoChange, double phiChange, double sigmaChange,
                                 boolean isWin, boolean isDraw) {
        if (isWin && isDraw) {
            throw new IllegalArgumentException("Result can't be a win and a draw");
        }
        double currentGlicko = team.getGlicko();
        double currentPhi = team.getPhi();
        double currentSigma = team.getSigma();
        double newGlicko = currentGlicko + glickoChange;
        double newPhi = currentPhi + phiChange;
        double newSigma = currentSigma + sigmaChange;
        team.setGlicko(newGlicko);
        team.setPhi(newPhi);
        team.setSigma(newSigma);
        team.setMatchesPlayed(team.getMatchesPlayed() + 1);
        if (!isWin && !isDraw) {
            team.setNumberLosses(team.getNumberLosses() + 1);
        } else if (isWin) {
            team.setNumberWins(team.getNumberWins() + 1);
        } else if (isDraw) {
            team.setNumberDraws(team.getNumberDraws() + 1);
        }
        return teamRepository.save(team);
    }


    public void setCurrentSubmission(Long teamId, Long submissionId) {
        if (!submissionService.isSubmissionValid(submissionId)) {
            throw new IllegalArgumentException("Submission is not valid");
        }
        Team team = teamRepository.findById(teamId).get();
        team.setCurrentSubmission(submissionService.getSubmissionReferenceById(submissionId));
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
    public void editTeam(Long teamId, EditTeamDto editTeamDto) {
        if (!teamRepository.existsById(teamId)) {
            throw new IllegalArgumentException("Team with id " + teamId + " does not exist");
        }
        Team team = teamRepository.findById(teamId).get();
        if (editTeamDto.getName() != null) team.setName(editTeamDto.getName());
        if (editTeamDto.getQuote() != null) team.setQuote(editTeamDto.getQuote());
        team.setDisplayMembers(editTeamDto.isDisplayMembers());

        teamRepository.save(team);
    }

    public boolean isTeamJoinable(Team team) {
        return team.getNumberPlayers() < 2;
    }

    public List<LeaderboardDTO> getLeaderboard() {
        AtomicInteger rank = new AtomicInteger(1);
        List<LeaderboardDTO> leaderboard = teamRepository.findAll().stream()
                .filter(team -> team.getCurrentSubmission() != null)
                .sorted(Comparator.comparing(Team::getGlicko).reversed())
                .map(team -> teamToLeaderboardDTO(team, rank.getAndIncrement()))
                .collect(Collectors.toList());
        return leaderboard;
    }

    public Page<LeaderboardDTO> getLeaderboard(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "glicko"));
        AtomicInteger rank = new AtomicInteger(1 + page * size);
        Page<Team> teamPage = teamRepository.findAllTeamsWithCurrentSubmission(pageable);
        return teamPage.map(team -> teamToLeaderboardDTO(team, rank.getAndIncrement()));
    }

    private LeaderboardDTO teamToLeaderboardDTO(Team team, int rank) {
        List<Player> teamPlayers = playerService.getPlayersByTeam(team.getId());
        List<String> playerNames = teamPlayers.stream().map(Player::getName).toList();

        LeaderboardDTO.LeaderboardDTOBuilder builder = LeaderboardDTO.builder();
        builder.teamUuid(team.getUuid().toString())
                .rank(rank)
                .glicko(team.getGlicko())
                .teamName(team.getName())
                .createdAt(team.getCreationDateTime())
                .quote(team.getQuote());

        if (team.isDisplayMembers()) {
            builder.members(playerNames);
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

    public Integer incrementTeamMembers(Long teamId) {
        Team team = teamRepository.findById(teamId).get();
        Integer currentNumber = team.getNumberPlayers();
        team.setNumberPlayers(currentNumber + 1);
        return currentNumber + 1;
    }

    public Integer decrementTeamMembers(Long teamId) {
        Team team = teamRepository.findById(teamId).get();
        if (team.getNumberPlayers() == 0) {
            throw new IllegalArgumentException("Team with id " + teamId + " has 0 players");
        }
        Integer currentNumber = team.getNumberPlayers();
        team.setNumberPlayers(currentNumber - 1);
        return currentNumber - 1;
    }


    public boolean isNameExist(String name) {
        return teamRepository.existsByName(name);
    }
}


