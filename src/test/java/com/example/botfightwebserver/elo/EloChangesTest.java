package com.example.botfightwebserver.elo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EloChangesTest {

    @Test
    void testDefaultConstructor() {
        EloChanges eloChanges = EloChanges.builder().build();
        assertEquals(0.0, eloChanges.getPlayer1Change());
        assertEquals(0.0, eloChanges.getPlayer2Change());
    }

    @Test
    void testAllArgsConstructor() {
        EloChanges eloChanges = EloChanges.builder()
                .player1Change(1.0)
                .player2Change(-2.0)
                .build();
        assertEquals(1.0, eloChanges.getPlayer1Change());
        assertEquals(-2.0, eloChanges.getPlayer2Change());
    }

    @Test
    void testSetters() {
        EloChanges eloChanges = EloChanges.builder().build();

        eloChanges.setPlayer1Change(15.7);
        eloChanges.setPlayer2Change(-15.7);

        assertEquals(15.7, eloChanges.getPlayer1Change());
        assertEquals(-15.7, eloChanges.getPlayer2Change());
    }
}