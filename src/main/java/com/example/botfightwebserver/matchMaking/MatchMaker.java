package com.example.botfightwebserver.matchMaking;

import com.example.botfightwebserver.gameMatch.GameMatchService;
import com.example.botfightwebserver.gameMatch.MAPS;
import com.example.botfightwebserver.gameMatch.MATCH_REASON;
import com.example.botfightwebserver.glicko.GlickoHistoryService;
import com.example.botfightwebserver.player.PlayerService;
import com.example.botfightwebserver.team.Team;
import com.example.botfightwebserver.team.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@RequiredArgsConstructor
@Component
public class MatchMaker {

    private final TeamService teamService;
    private final GameMatchService gameMatchService;
    private final GlickoHistoryService glickoHistoryService;
    private final MatchMakingEventService matchMakingEventService;

    private static final Random RANDOM = new Random();
    private static final List<MAPS> RANKED_MAPS = List.of(MAPS.values());

    public void generateMatches(boolean saveHistory, MATCHMAKING_REASON reason) {
        List<Team> playableTeams = teamService.getTeamsWithSubmission();
        final List<Team> teams =
            playableTeams.stream().sorted(Comparator.comparing(Team::getGlicko).reversed()).toList();

        if (saveHistory) {
            playableTeams.forEach(team -> glickoHistoryService.save(team.getId(), team.getGlicko()));
        }

        List<int[]> edges;
        if (teams.size() <= 4) {
            edges = new ArrayList<>();
            for (int j = 0; j < teams.size(); j++) {
                for (int i = 0; i < j; i++) {
                    edges.add(new int[] {i, j});
                }
            }
        } else {
            edges = generate4RegularGraph(teams.size());
        }
        Random random = new Random();
        for (int i = 0; i < edges.size(); i++) {
            if (random.nextBoolean()) {
                int[] edge = edges.get(i);
                int temp = edge[0];
                edge[0] = edge[1];
                edge[1] = temp;
            }
        }

        Collections.shuffle(edges, random);
        matchMakingEventService.createEvent(playableTeams.size(), edges.size(), reason);
        edges.stream().forEach((edge) -> {
            Team teamOne = teams.get(edge[0]);
            Team teamTwo = teams.get(edge[1]);
            gameMatchService.submitGameMatch(teamOne.getId(), teamTwo.getId(), teamOne.getCurrentSubmission().getId(),
                teamTwo.getCurrentSubmission().getId(), MATCH_REASON.LADDER, randomSelectMap());
        });
    }

    public List<int[]> generate4RegularGraph(int n) {
        if (n < 5) {
            throw new IllegalArgumentException("Not enough nodes for 4-regular graph");
        }

        Random random = new Random();

        String partitionBaseCases = ".....567895566755565555565";
        List<Integer> partitionSizes = new ArrayList<>();
        int remaining = n;

        while (remaining > 0) {
            int m;
            if (remaining < partitionBaseCases.length()) {
                m = Character.getNumericValue(partitionBaseCases.charAt(remaining));
            } else {
                m = random.nextInt(2) + 5;
                System.out.println(m);
            }
            partitionSizes.add(m);
            remaining -= m;
        }
        Collections.shuffle(partitionSizes, random);

        Map<Integer, String> predetermined = new HashMap<>();
        predetermined.put(5, "01 02 03 04 12 13 14 23 24 34");
        predetermined.put(6, "01 02 03 04 12 13 15 24 25 34 35 45");
        predetermined.put(7, "01 02 03 04 12 13 14 25 26 35 36 45 46 56");
        predetermined.put(8, "01 02 03 04 12 13 14 25 26 35 37 46 47 56 57 67");
        predetermined.put(9, "01 02 03 04 12 13 14 23 25 36 47 48 56 57 58 67 68 78");

        Map<Integer, List<Integer>> adjList = new HashMap<>();
        for (int i = 0; i < n; i++) {
            adjList.put(i, new ArrayList<>());
        }

        int start = 0;
        for (int m : partitionSizes) {
            String[] edges = predetermined.get(m).split(" ");
            for (String edge : edges) {
                int i = Character.getNumericValue(edge.charAt(0));
                int j = Character.getNumericValue(edge.charAt(1));
                adjList.get(start + i).add(start + j);
                adjList.get(start + j).add(start + i);
            }
            start += m;
        }

        start = 0;
        for (int i = 0; i < partitionSizes.size() - 1; i++) {
            int m = partitionSizes.get(i);
            int a = start + m - 1;
            int b = start + m;

            List<Integer> aNeighbors = new ArrayList<>(adjList.get(a));
            Collections.sort(aNeighbors);
            int c = aNeighbors.get(random.nextInt(aNeighbors.size() - 1) + 1);

            List<Integer> bNeighbors = new ArrayList<>(adjList.get(b));
            Collections.sort(bNeighbors);
            int d = bNeighbors.get(random.nextInt(bNeighbors.size() - 1));

            removeEdge(adjList, a, c);
            removeEdge(adjList, b, d);

            addEdge(adjList, b, c);
            addEdge(adjList, a, d);

            start += m;
        }

        List<int[]> edges = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j : adjList.get(i)) {
                if (j > i) {
                    edges.add(new int[] {i, j});
                }
            }
        }
        return edges;
    }

    private static void removeEdge(Map<Integer, List<Integer>> adjList, int u, int v) {
        adjList.get(u).remove(Integer.valueOf(v));
        adjList.get(v).remove(Integer.valueOf(u));
    }

    private static void addEdge(Map<Integer, List<Integer>> adjList, int u, int v) {
        adjList.get(u).add(v);
        adjList.get(v).add(u);
    }

    private static String randomSelectMap() {
        return RANKED_MAPS.get(RANDOM.nextInt(RANKED_MAPS.size())).toMapName();
    }
}

