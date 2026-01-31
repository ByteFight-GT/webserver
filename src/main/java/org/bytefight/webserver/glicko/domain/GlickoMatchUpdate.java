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

        public void setTeam(Team team) {
            this.team = team;
        }

        public void setOldRating(double oldRating) {
            this.oldRating = oldRating;
        }

        public void setOldRd(double oldRd) {
            this.oldRd = oldRd;
        }

        public void setOldVolatility(double oldVolatility) {
            this.oldVolatility = oldVolatility;
        }

        public void setNewRating(double newRating) {
            this.newRating = newRating;
        }

        public void setNewRd(double newRd) {
            this.newRd = newRd;
        }

        public void setNewVolatility(double newVolatility) {
            this.newVolatility = newVolatility;
        }

        public void setMatchesPlayedDelta(int matchesPlayedDelta) {
            this.matchesPlayedDelta = matchesPlayedDelta;
        }

        public void setWinsDelta(int winsDelta) {
            this.winsDelta = winsDelta;
        }

        public void setLossesDelta(int lossesDelta) {
            this.lossesDelta = lossesDelta;
        }

        public void setDrawsDelta(int drawsDelta) {
            this.drawsDelta = drawsDelta;
        }
    }
}
