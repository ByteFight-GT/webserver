package org.bytefight.webserver.tournament.application;

import org.bytefight.webserver.tournament.domain.Tournament;
import org.bytefight.webserver.tournament.domain.TournamentBracketType;
import org.bytefight.webserver.tournament.domain.TournamentEntry;
import org.bytefight.webserver.tournament.domain.TournamentMatch;
import org.bytefight.webserver.tournament.domain.TournamentMatchState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds a double-elimination bracket graph.
 *
 * Output:
 * - Winners bracket rounds
 * - Losers bracket rounds
 * - Grand final and optional reset match
 *
 * Storage notes:
 * - TournamentMatch rows act as nodes in a graph
 * - nextWinnerMatchId/nextLoserMatchId encode directed edges between nodes
 */
@Service
@RequiredArgsConstructor
public class TournamentBracketBuilder {

    /**
     * Builds the bracket in memory.
     *
     * Steps:
     * - Expand team count to next power of two (byes)
     * - Generate seed order (1 vs N, 2 vs N-1, etc.)
     * - Create winners and losers round match nodes
     * - Add grand final + reset node
     *
     * Wiring is done by separate methods so ids can be assigned first.
     */
    public TournamentBracketGraph buildBracket(Tournament tournament, List<TournamentEntry> entries) {
        int teamCount = entries.size();
        int bracketSize = nextPowerOfTwo(teamCount);
        tournament.setBracketSize(bracketSize);

        int rounds = (int) (Math.log(bracketSize) / Math.log(2));

        List<List<TournamentMatch>> winnersRounds = new ArrayList<>();
        List<List<TournamentMatch>> losersRounds = new ArrayList<>();

        Map<Integer, TournamentEntry> entryBySeed = entries.stream()
                .collect(Collectors.toMap(TournamentEntry::getSeed, e -> e));

        List<Integer> seedOrder = generateSeedOrder(bracketSize);
        List<TournamentEntry> orderedEntries = seedOrder.stream()
                .map(seed -> entryBySeed.getOrDefault(seed, null))
                .toList();

        List<TournamentMatch> round1 = new ArrayList<>();
        for (int i = 0; i < bracketSize / 2; i++) {
            TournamentEntry teamOne = orderedEntries.get(i * 2);
            TournamentEntry teamTwo = orderedEntries.get(i * 2 + 1);
            round1.add(createMatch(tournament, TournamentBracketType.WINNERS, 1, i + 1, teamOne, teamTwo));
        }
        winnersRounds.add(round1);

        for (int r = 2; r <= rounds; r++) {
            int matches = bracketSize / (1 << r);
            List<TournamentMatch> round = new ArrayList<>();
            for (int i = 0; i < matches; i++) {
                round.add(createMatch(tournament, TournamentBracketType.WINNERS, r, i + 1, null, null));
            }
            winnersRounds.add(round);
        }

        int losersRoundsCount = (2 * rounds) - 1;
        for (int i = 1; i <= rounds - 1; i++) {
            int matches = 1 << (rounds - i - 1);
            List<TournamentMatch> oddRound = new ArrayList<>();
            List<TournamentMatch> evenRound = new ArrayList<>();
            for (int j = 0; j < matches; j++) {
                oddRound.add(createMatch(tournament, TournamentBracketType.LOSERS, (2 * i) - 1, j + 1, null, null));
                evenRound.add(createMatch(tournament, TournamentBracketType.LOSERS, (2 * i), j + 1, null, null));
            }
            losersRounds.add(oddRound);
            losersRounds.add(evenRound);
        }
        if (losersRoundsCount > 0) {
            List<TournamentMatch> last = new ArrayList<>();
            last.add(createMatch(tournament, TournamentBracketType.LOSERS, losersRoundsCount, 1, null, null));
            losersRounds.add(last);
        }

        TournamentMatch grandFinal = createMatch(tournament, TournamentBracketType.GRAND_FINAL, 1, 1, null, null);
        TournamentMatch grandFinalReset = createMatch(tournament, TournamentBracketType.GRAND_FINAL_RESET, 1, 1, null, null);

        List<TournamentMatch> allMatches = new ArrayList<>();
        winnersRounds.forEach(allMatches::addAll);
        losersRounds.forEach(allMatches::addAll);
        allMatches.add(grandFinal);
        allMatches.add(grandFinalReset);

        return new TournamentBracketGraph(winnersRounds, losersRounds, grandFinal, grandFinalReset, allMatches);
    }

    /**
     * Wires winner edges in the winners bracket.
     * Winner of round r goes to round r+1 at slot (1 or 2).
     */
    public void wireWinnersAdvancement(List<List<TournamentMatch>> winnersRounds) {
        for (int r = 0; r < winnersRounds.size() - 1; r++) {
            List<TournamentMatch> round = winnersRounds.get(r);
            List<TournamentMatch> next = winnersRounds.get(r + 1);
            for (int i = 0; i < round.size(); i++) {
                TournamentMatch match = round.get(i);
                TournamentMatch target = next.get(i / 2);
                match.setNextWinnerMatchId(target.getId());
                match.setNextWinnerSlot(i % 2 == 0 ? 1 : 2);
            }
        }
    }

    /**
     * Wires loser edges and losers-bracket progression.
     *
     * Pattern:
     * - Winners round losers feed into specific losers rounds
     * - Losers bracket alternates merge and advancement rounds
     */
    public void wireLosersAdvancement(List<List<TournamentMatch>> winnersRounds, List<List<TournamentMatch>> losersRounds) {
        if (losersRounds.isEmpty()) {
            return;
        }

        List<TournamentMatch> w1 = winnersRounds.get(0);
        List<TournamentMatch> l1 = losersRounds.get(0);
        for (int i = 0; i < w1.size(); i++) {
            TournamentMatch match = w1.get(i);
            TournamentMatch target = l1.get(i / 2);
            match.setNextLoserMatchId(target.getId());
            match.setNextLoserSlot(i % 2 == 0 ? 1 : 2);
        }

        for (int r = 1; r < winnersRounds.size(); r++) {
            int losersIndex = (2 * (r + 1)) - 2;
            if (losersIndex - 1 >= losersRounds.size()) {
                continue;
            }
            List<TournamentMatch> wr = winnersRounds.get(r);
            List<TournamentMatch> lr = losersRounds.get(losersIndex - 1);
            for (int i = 0; i < wr.size(); i++) {
                TournamentMatch match = wr.get(i);
                TournamentMatch target = lr.get(i);
                match.setNextLoserMatchId(target.getId());
                match.setNextLoserSlot(2);
            }
        }

        for (int l = 0; l < losersRounds.size() - 1; l++) {
            List<TournamentMatch> round = losersRounds.get(l);
            List<TournamentMatch> next = losersRounds.get(l + 1);
            if ((l + 1) % 2 == 1) {
                for (int i = 0; i < round.size(); i++) {
                    TournamentMatch match = round.get(i);
                    TournamentMatch target = next.get(i);
                    match.setNextWinnerMatchId(target.getId());
                    match.setNextWinnerSlot(1);
                }
            } else {
                for (int i = 0; i < round.size(); i++) {
                    TournamentMatch match = round.get(i);
                    TournamentMatch target = next.get(i / 2);
                    match.setNextWinnerMatchId(target.getId());
                    match.setNextWinnerSlot(i % 2 == 0 ? 1 : 2);
                }
            }
        }
    }

    /**
     * Connects winners-bracket champion and losers-bracket champion
     * to the grand final match.
     */
    public void wireLosersToGrandFinal(List<List<TournamentMatch>> winnersRounds, List<List<TournamentMatch>> losersRounds, TournamentMatch grandFinal) {
        List<TournamentMatch> winnersFinal = winnersRounds.get(winnersRounds.size() - 1);
        TournamentMatch winnersFinalMatch = winnersFinal.get(0);
        winnersFinalMatch.setNextWinnerMatchId(grandFinal.getId());
        winnersFinalMatch.setNextWinnerSlot(1);

        if (!losersRounds.isEmpty()) {
            TournamentMatch losersFinal = losersRounds.get(losersRounds.size() - 1).get(0);
            losersFinal.setNextWinnerMatchId(grandFinal.getId());
            losersFinal.setNextWinnerSlot(2);
        }
    }

    /**
     * Grand-final reset: if winners-bracket champion loses once,
     * a final reset match decides the champion.
     */
    public void wireGrandFinalReset(TournamentMatch grandFinal, TournamentMatch grandFinalReset) {
        grandFinal.setNextLoserMatchId(grandFinalReset.getId());
        grandFinal.setNextLoserSlot(1);
    }

    private TournamentMatch createMatch(Tournament tournament,
                                        TournamentBracketType bracketType,
                                        int round,
                                        int matchIndex,
                                        TournamentEntry teamOne,
                                        TournamentEntry teamTwo) {
        return TournamentMatch.builder()
                .tournament(tournament)
                .bracketType(bracketType)
                .roundNumber(round)
                .matchIndex(matchIndex)
                .teamOneEntry(teamOne)
                .teamTwoEntry(teamTwo)
                .state(TournamentMatchState.PENDING)
                .build();
    }

    /**
     * Calculates the next power-of-two bracket size.
     * Used to generate byes for uneven team counts.
     */
    private static int nextPowerOfTwo(int n) {
        int v = 1;
        while (v < n) {
            v <<= 1;
        }
        return v;
    }

    /**
     * Generates seed order for a power-of-two bracket.
     * Example for size=4: [1,4,2,3]
     */
    private static List<Integer> generateSeedOrder(int size) {
        List<Integer> seeds = new ArrayList<>();
        seeds.add(1);
        seeds.add(2);
        int currentSize = 2;
        while (currentSize < size) {
            int nextSize = currentSize * 2;
            List<Integer> next = new ArrayList<>(nextSize);
            for (int seed : seeds) {
                next.add(seed);
                next.add(nextSize + 1 - seed);
            }
            seeds = next;
            currentSize = nextSize;
        }
        return seeds;
    }
}
