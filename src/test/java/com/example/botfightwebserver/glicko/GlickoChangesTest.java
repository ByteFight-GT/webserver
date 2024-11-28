package com.example.botfightwebserver.glicko;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GlickoChangesTest {

    @Test
    void testDefaultConstructor() {
        GlickoChanges glickoChanges = GlickoChanges.builder().build();

        assertEquals(0.0, glickoChanges.getTeam1Change());
        assertEquals(0.0, glickoChanges.getTeam2Change());
        assertEquals(0.0, glickoChanges.getTeam1PhiChange());
        assertEquals(0.0, glickoChanges.getTeam2PhiChange());
        assertEquals(0.0, glickoChanges.getTeam1SigmaChange());
        assertEquals(0.0, glickoChanges.getTeam2SigmaChange());
    }

    @Test
    void testAllArgsConstructor() {
        GlickoChanges glickoChanges = GlickoChanges.builder()
                .team1Change(1.0)
                .team2Change(-2.0)
                .team1PhiChange(-10.0)
                .team2PhiChange(-15.0)
                .team1SigmaChange(0.01)
                .team2SigmaChange(0.02)
                .build();

        assertEquals(1.0, glickoChanges.getTeam1Change());
        assertEquals(-2.0, glickoChanges.getTeam2Change());
        assertEquals(-10.0, glickoChanges.getTeam1PhiChange());
        assertEquals(-15.0, glickoChanges.getTeam2PhiChange());
        assertEquals(0.01, glickoChanges.getTeam1SigmaChange());
        assertEquals(0.02, glickoChanges.getTeam2SigmaChange());
    }

    @Test
    void testSettersAndGetters() {
        GlickoChanges glickoChanges = GlickoChanges.builder().build();

        glickoChanges.setTeam1Change(15.7);
        glickoChanges.setTeam2Change(-15.7);
        glickoChanges.setTeam1PhiChange(-12.5);
        glickoChanges.setTeam2PhiChange(-8.3);
        glickoChanges.setTeam1SigmaChange(0.05);
        glickoChanges.setTeam2SigmaChange(0.04);

        assertEquals(15.7, glickoChanges.getTeam1Change());
        assertEquals(-15.7, glickoChanges.getTeam2Change());
        assertEquals(-12.5, glickoChanges.getTeam1PhiChange());
        assertEquals(-8.3, glickoChanges.getTeam2PhiChange());
        assertEquals(0.05, glickoChanges.getTeam1SigmaChange());
        assertEquals(0.04, glickoChanges.getTeam2SigmaChange());
    }
}
