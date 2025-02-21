package com.example.botfightwebserver.tournament;

import com.example.botfightwebserver.config.ClockConfig;
import com.example.botfightwebserver.player.PlayerService;
import com.example.botfightwebserver.team.Team;
import com.google.type.DateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TournamentService {

    private final String CHALLONGE_API_BASE_URL = "https://api.challonge.com/v1/tournaments";
    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${CHALLONGE_API_KEY}")
    private String apiKey;

    private final ClockConfig clockConfig;
    private final TournamentRepository tournamentRepository;
    private final PlayerService playerService;
    private final TournamentTeamService tournamentTeamService;

    public Tournament getTournament(Long tournamentId) {
        return tournamentRepository.findById(tournamentId).orElse(null);
    }

    public Tournament saveTournament(Tournament tournament) {
        return tournamentRepository.save(tournament);
    }

    public Tournament createTournament(Tournament tournament) {
        Map<String, Object> tournamentData = new HashMap<>();
        Map<String, Object> wrapper = new HashMap<>();

        tournamentData.put("name", tournament.getName());
        tournamentData.put("description", tournament.getDescription());
        tournamentData.put("url", generateUrlSlug(tournament.getName()));
        tournamentData.put("tournament_type", tournament.getTournamentType().toChallongeType());

        wrapper.put("tournament", tournamentData);
        wrapper.put("api_key", apiKey);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(wrapper, headers);
        String url = CHALLONGE_API_BASE_URL + ".json";
        Map<String, Object> challongeResponse = restTemplate.postForObject(
            url,
            requestEntity,
            Map.class
        );

        if (challongeResponse != null && challongeResponse.get("tournament") != null) {
            Map<String, Object> tournamentResponse = (Map<String, Object>) challongeResponse.get("tournament");
            tournament.setChallongeId(((Number) tournamentResponse.get("id")).longValue());
        }

        return tournamentRepository.save(tournament);
    }

    public void addPlayers(Long tournamentId, List<Team> teams) {
        List<Team> sortedTeams = teams.stream().sorted((a, b) -> Double.compare(b.getGlicko(), a.getGlicko())).toList();

        Tournament tournament = tournamentRepository.findById(tournamentId).orElseThrow(
            () -> new IllegalArgumentException("Invalid tournament id: " + tournamentId)
        );


        Map<String, Object> wrapper = new HashMap<>();
        List<Object> participants = new ArrayList<>();

        System.out.println(sortedTeams.size());
        for (int i = 0; i < sortedTeams.size(); i++) {
            Map<String, Object> participantInfo = new HashMap<>();
            participantInfo.put("name", sortedTeams.get(i).getName());
            participantInfo.put("seed", i + 1);
            participants.add(participantInfo);
        }

        wrapper.put("participants", participants);
        wrapper.put("api_key", apiKey);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(wrapper, headers);
        String url = CHALLONGE_API_BASE_URL + "/" + tournament.getChallongeId() + "/participants/bulk_add.json";
        List<Map<String, Object>> response = restTemplate.postForObject(
            url,
            requestEntity,
            List.class
        );
        List<TournamentTeam> tournamentTeams = new ArrayList<>();
        for (int i = 0; i < response.size(); i++) {
            Map<String, Object> participantData = (Map<String, Object>) response.get(i).get("participant");
            tournamentTeams.add(TournamentTeam.builder()
                .team(sortedTeams.get(i))
                .tournament(tournament)
                .challongeParticipantId(((Number) participantData.get("id")).longValue())
                .build());
        }

        tournamentTeamService.savePlayers(tournamentTeams);
    }


    public Tournament startTournament(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
            .orElseThrow(() -> new RuntimeException("Tournament not found"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> params = new HashMap<>();
        params.put("api_key", apiKey);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(params, headers);

        restTemplate.postForObject(
            CHALLONGE_API_BASE_URL + "/" + tournament.getChallongeId() + "/start",
            requestEntity,
            Map.class
        );

        tournament.setStartDateTime(LocalDateTime.now(clockConfig.clock()));
        tournament.setStatus(TOURNAMENT_STATUS.IN_PROGRESS);
        return tournamentRepository.save(tournament);
    }


    public Tournament finalizeTournament(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
            .orElseThrow(() -> new RuntimeException("Tournament not found"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> params = new HashMap<>();
        params.put("api_key", apiKey);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(params, headers);

        restTemplate.postForObject(
            CHALLONGE_API_BASE_URL + "/" + tournament.getChallongeId() + "/finalize",
            requestEntity,
            Map.class
        );

        tournament.setStatus(TOURNAMENT_STATUS.FINISHED);
        return tournamentRepository.save(tournament);
    }

    public void updateMatchResult(Long tournamentId, Long challongeMatchId, int player1Score, int player2Score, String winnerId) {
        Tournament tournament = tournamentRepository.findById(tournamentId).orElseThrow(
            () -> new IllegalArgumentException("Invalid tournament id: " + tournamentId)
        );

        Map<String, Object> matchData = new HashMap<>();
        Map<String, Object> wrapper = new HashMap<>();

        matchData.put("scores_csv", player1Score + "-" + player2Score);
        matchData.put("winner_id", winnerId);

        wrapper.put("api_key", apiKey);
        wrapper.put("match", matchData);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(wrapper, headers);

        String url = CHALLONGE_API_BASE_URL + "/" + tournament.getChallongeId() +
            "/matches/" + challongeMatchId + ".json";

        restTemplate.put(url, requestEntity);
    }

    private List<ChallongeMatchDTO> processMatchesResponse(Map<String, Object>[] matchesData) {
        List<ChallongeMatchDTO> matches = new ArrayList<>();
        System.out.println(matchesData);
        if (matchesData == null) return matches;

        for (Map<String, Object> matchData : matchesData) {
            Map<String, Object> match = (Map<String, Object>) matchData.get("match");
            if (match.get("state").equals("open")) {
                matches.add(ChallongeMatchDTO.builder()
                    .round(((Number) match.get("round")).intValue())
                    .matchId(match.get("id").toString())
                    .challongePlayer1Id(Long.parseLong(match.get("player1_id").toString()))
                    .challongePlayer2Id(Long.parseLong(match.get("player2_id").toString()))
                    .state(TOURNAMENT_MATCH_STATES.fromString((String) match.get("state")))
                    .build());
            }
        }
        return matches;
    }

    public List<ChallongeMatchDTO> getTournamentMatches(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId).orElseThrow(
            () -> new IllegalArgumentException("Invalid tournament id: " + tournamentId)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String url = CHALLONGE_API_BASE_URL + "/" + tournament.getChallongeId() +
            "/matches.json?api_key=" + apiKey;
        System.out.println(url);
        Map<String, Object>[] matches = restTemplate.getForObject(url, Map[].class);
        return processMatchesResponse(matches);
    }

    private String generateUrlSlug(String name) {
        LocalDateTime now = LocalDateTime.now(clockConfig.clock());
        String url = name + now.toString();
        System.out.println(url);
        return url.toLowerCase()
            .replaceAll("[^a-z0-9]", "_")
            .replaceAll("_+", "_")
            .replaceAll("^_|_$", "");
    }

}
