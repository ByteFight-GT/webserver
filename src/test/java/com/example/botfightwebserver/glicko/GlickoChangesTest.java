package com.example.botfightwebserver.glicko;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GlickoChangesTest {

    @Test
    void testDefaultConstructor() {
        GlickoChanges glickoChanges = GlickoChanges.builder().build();

        assertEquals(0.0, glickoChanges.getPlayer1Change());
        assertEquals(0.0, glickoChanges.getPlayer2Change());
        assertEquals(0.0, glickoChanges.getPlayer1PhiChange());
        assertEquals(0.0, glickoChanges.getPlayer2PhiChange());
        assertEquals(0.0, glickoChanges.getPlayer1SigmaChange());
        assertEquals(0.0, glickoChanges.getPlayer2SigmaChange());
    }

    @Test
    void testAllArgsConstructor() {
        GlickoChanges glickoChanges = GlickoChanges.builder()
                .player1Change(1.0)
                .player2Change(-2.0)
                .player1PhiChange(-10.0)
                .player2PhiChange(-15.0)
                .player1SigmaChange(0.01)
                .player2SigmaChange(0.02)
                .build();

        assertEquals(1.0, glickoChanges.getPlayer1Change());
        assertEquals(-2.0, glickoChanges.getPlayer2Change());
        assertEquals(-10.0, glickoChanges.getPlayer1PhiChange());
        assertEquals(-15.0, glickoChanges.getPlayer2PhiChange());
        assertEquals(0.01, glickoChanges.getPlayer1SigmaChange());
        assertEquals(0.02, glickoChanges.getPlayer2SigmaChange());
    }

    @Test
    void testSettersAndGetters() {
        GlickoChanges glickoChanges = GlickoChanges.builder().build();

        glickoChanges.setPlayer1Change(15.7);
        glickoChanges.setPlayer2Change(-15.7);
        glickoChanges.setPlayer1PhiChange(-12.5);
        glickoChanges.setPlayer2PhiChange(-8.3);
        glickoChanges.setPlayer1SigmaChange(0.05);
        glickoChanges.setPlayer2SigmaChange(0.04);

        assertEquals(15.7, glickoChanges.getPlayer1Change());
        assertEquals(-15.7, glickoChanges.getPlayer2Change());
        assertEquals(-12.5, glickoChanges.getPlayer1PhiChange());
        assertEquals(-8.3, glickoChanges.getPlayer2PhiChange());
        assertEquals(0.05, glickoChanges.getPlayer1SigmaChange());
        assertEquals(0.04, glickoChanges.getPlayer2SigmaChange());
    }
}
