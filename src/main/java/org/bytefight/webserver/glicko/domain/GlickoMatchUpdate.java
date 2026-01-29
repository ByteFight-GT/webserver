package org.bytefight.webserver.glicko.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bytefight.webserver.competition.domain.Competition;
import org.bytefight.webserver.team.domain.Team;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GlickoMatchUpdate {
    private Competition competition;
    private Ladder ladder;
    private TeamUpdate teamA;
    private TeamUpdate teamB;

    public Competition getCompetition() {
        return competition;
    }

    public Ladder getLadder() {
        return ladder;
    }

    public TeamUpdate getTeamA() {
        return teamA;
    }

    public TeamUpdate getTeamB() {
        return teamB;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamUpdate {
        private Team team;
        private double oldRating;
        private double oldRd;
        private double oldVolatility;
        private double newRating;
        private double newRd;
        private double newVolatility;
        private int matchesPlayedDelta;
        private int winsDelta;
        private int lossesDelta;
        private int drawsDelta;

        public Team getTeam() {
            return team;
        }

        public double getOldRating() {
            return oldRating;
        }

        public double getOldRd() {
            return oldRd;
        }

        public double getOldVolatility() {
            return oldVolatility;
        }

        public double getNewRating() {
            return newRating;
        }

        public double getNewRd() {
            return newRd;
        }

        public double getNewVolatility() {
            return newVolatility;
        }

        public int getMatchesPlayedDelta() {
            return matchesPlayedDelta;
        }

        public int getWinsDelta() {
            return winsDelta;
        }

        public int getLossesDelta() {
            return lossesDelta;
        }

        public int getDrawsDelta() {
            return drawsDelta;
        }
    }
}
