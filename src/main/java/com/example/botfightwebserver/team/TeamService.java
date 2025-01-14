package com.example.botfightwebserver.team;

import com.example.botfightwebserver.submission.Submission;
import com.example.botfightwebserver.submission.SubmissionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamService {

    private final TeamRepository teamRepository;
    private final SubmissionService submissionService;

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

    public Team getReferenceById(Long id) {
        return teamRepository.getReferenceById(id);
    }

    public TeamDTO getDTOById(Long id) {
        return TeamDTO.fromEntity(teamRepository.getReferenceById(id));
    }


    public Team createTeam(String name) {
        if (teamRepository.existsByName(name)) {
            throw new IllegalArgumentException("Team with name " + name + " already exists");
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
        if (team1Id.equals(team2Id)) {
            throw new IllegalArgumentException("Teams must be different");
        }
    }

    public Team updateAfterLadderMatch(Team team, double glickoChange, double phiChange, double sigmaChange,
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

        // Create a pageable request with sorting by Glicko in descending order
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "glicko"));

        // Fetch the teams for the specified page
        Page<Team> teamPage = teamRepository.findAll(pageable);

        // Return the teams as a list
        return teamPage.getContent();
    }
}


