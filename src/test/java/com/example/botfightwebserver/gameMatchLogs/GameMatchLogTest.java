package com.example.botfightwebserver.gameMatchLogs;

import com.example.botfightwebserver.PersistentTestBase;
import com.example.botfightwebserver.player.Player;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase
@ActiveProfiles("test")
class GameMatchLogTest extends PersistentTestBase {

    @Test
    void testBuilderWithAllFields() {
        GameMatchLog gameMatchLog = GameMatchLog.builder()
                .matchId(700L)
                .matchLog("Player 1 won!")
                .player1GlickoChange(10.0)
                .player2GlickoChange(-10.0)
                .build();

        GameMatchLog persistedAndReturnedEntity = persistAndReturnEntity(gameMatchLog);

        assertNotNull(persistedAndReturnedEntity.getId());
        assertEquals(700L, persistedAndReturnedEntity.getMatchId());
        assertEquals("Player 1 won!", persistedAndReturnedEntity.getMatchLog());
        assertEquals(10.0, persistedAndReturnedEntity.getPlayer1GlickoChange());
        assertEquals(-10.0, persistedAndReturnedEntity.getPlayer2GlickoChange());
    }

    @Test
    void testBuilderNoFields() {
        GameMatchLog gameMatchLog = GameMatchLog.builder().build();

        GameMatchLog persistedAndReturnedEntity = persistAndReturnEntity(gameMatchLog);

        assertNotNull(persistedAndReturnedEntity.getId());
        assertNull(persistedAndReturnedEntity.getMatchId());
        assertNull(persistedAndReturnedEntity.getMatchLog());
        assertNull(persistedAndReturnedEntity.getPlayer1GlickoChange());
        assertNull(persistedAndReturnedEntity.getPlayer2GlickoChange());
    }
}