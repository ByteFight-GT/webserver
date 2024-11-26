package com.example.botfightwebserver.gameMatchLogs;

import com.example.botfightwebserver.PersistentTestBase;
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
                .matchLog("Team 1 won!")
                .team1GlickoChange(10.0)
                .team2GlickoChange(-10.0)
                .build();

        GameMatchLog persistedAndReturnedEntity = persistAndReturnEntity(gameMatchLog);

        assertNotNull(persistedAndReturnedEntity.getId());
        assertEquals(700L, persistedAndReturnedEntity.getMatchId());
        assertEquals("Team 1 won!", persistedAndReturnedEntity.getMatchLog());
        assertEquals(10.0, persistedAndReturnedEntity.getTeam1GlickoChange());
        assertEquals(-10.0, persistedAndReturnedEntity.getTeam2GlickoChange());
    }

    @Test
    void testBuilderNoFields() {
        GameMatchLog gameMatchLog = GameMatchLog.builder().build();

        GameMatchLog persistedAndReturnedEntity = persistAndReturnEntity(gameMatchLog);

        assertNotNull(persistedAndReturnedEntity.getId());
        assertNull(persistedAndReturnedEntity.getMatchId());
        assertNull(persistedAndReturnedEntity.getMatchLog());
        assertNull(persistedAndReturnedEntity.getTeam1GlickoChange());
        assertNull(persistedAndReturnedEntity.getTeam2GlickoChange());
    }
}