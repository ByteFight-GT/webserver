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
        GameMatchLog gameMatchLog = new GameMatchLog();

        gameMatchLog.setMatchId(700L);
        gameMatchLog.setMatchLog("Player 1 won!");
        gameMatchLog.setPlayer1EloChange(10.0);
        gameMatchLog.setPlayer2EloChange(-10.0);

        GameMatchLog persistedAndReturnedEntity = persistAndReturnEntity(gameMatchLog);

        assertEquals(700L, persistedAndReturnedEntity.getMatchId());
        assertEquals("Player 1 won!", persistedAndReturnedEntity.getMatchLog());
        assertEquals(10.0, persistedAndReturnedEntity.getPlayer1EloChange());
        assertEquals(-10.0, persistedAndReturnedEntity.getPlayer2EloChange());
    }

    @Test
    void testBuilderNoFields() {
        GameMatchLog gameMatchLog = new GameMatchLog();

        GameMatchLog persistedAndReturnedEntity = persistAndReturnEntity(gameMatchLog);

        assertEquals(1L, persistedAndReturnedEntity.getId());
        assertNull(persistedAndReturnedEntity.getMatchId());
        assertNull(persistedAndReturnedEntity.getMatchLog());
        assertNull(persistedAndReturnedEntity.getPlayer1EloChange());
        assertNull(persistedAndReturnedEntity.getPlayer2EloChange());
    }
}