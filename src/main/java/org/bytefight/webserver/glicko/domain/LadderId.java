package org.bytefight.webserver.glicko.domain;

import java.io.Serializable;
import java.util.Objects;

public class LadderId implements Serializable {
    private Long competition;
    private String ladder;

    public LadderId() {}

    public LadderId(Long competition, String ladder) {
        this.competition = competition;
        this.ladder = ladder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LadderId ladderId = (LadderId) o;
        return Objects.equals(competition, ladderId.competition)
                && Objects.equals(ladder, ladderId.ladder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(competition, ladder);
    }
}
