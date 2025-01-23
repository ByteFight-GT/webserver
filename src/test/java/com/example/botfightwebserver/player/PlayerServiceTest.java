package com.example.botfightwebserver.player;

import com.example.botfightwebserver.PersistentTestBase;
import com.example.botfightwebserver.team.TeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@DataJpaTest
@AutoConfigureTestDatabase
@ActiveProfiles("test")
class PlayerServiceTest extends PersistentTestBase {

    @Autowired
    private PlayerRepository playerRepository;

    private PlayerService playerService;

    @BeforeEach
    void setUp() {
        playerService = new PlayerService(playerRepository);
    }

    @Test
    void testGetPlayers() {
        Player player = Player.builder()
            .name("Test")
            .email("test@email.com")
            .teamId(1L)
            .build();

        persistEntity(player);

        List<Player> players = playerService.getPlayers();

        assertEquals(1, players.size());
        assertEquals(player.getName(), players.get(0).getName());
        assertEquals(player.getEmail(), players.get(0).getEmail());
        assertEquals(player.getTeamId(), players.get(0).getTeamId());
    }

    @Test
    void testCreatePlayer() {
        Player player = playerService.createPlayer("Test", "test@email.com", 1L);

        assertNotNull(player);
        assertEquals("Test", player.getName());
        assertEquals("test@email.com", player.getEmail());
        assertEquals(1L, player.getTeamId());
    }

    @Test
    void testCreatePlayer_DuplicateEmail() {
        persistEntity(Player.builder()
            .email("test@email.com")
            .build());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> playerService.createPlayer("Test", "test@email.com", 1L)
        );

        assertEquals("Player with email test@email.com already exists", exception.getMessage());
    }

    @Test
    void testSetPlayerTeam() {
        Player player = persistAndReturnEntity(Player.builder()
            .name("Test")
            .build());

        Player updated = playerService.setPlayerTeam(player.getId(), 2L);

        assertEquals(2L, updated.getTeamId());
    }

    @Test
    void testGetPlayer() {
        Player player = persistAndReturnEntity(Player.builder()
            .name("Test")
            .email("test@email.com")
            .teamId(1L)
            .build());

        Player found = playerService.getPlayer(player.getId());

        assertNotNull(found);
        assertEquals(player.getName(), found.getName());
        assertEquals(player.getEmail(), found.getEmail());
        assertEquals(player.getTeamId(), found.getTeamId());
    }

    @Test
    void testGetPlayersByTeam() {
        Player player1 = persistAndReturnEntity(Player.builder()
            .name("Test1")
            .email("test1@email.com")
            .teamId(1L)
            .build());

        Player player2 = persistAndReturnEntity(Player.builder()
            .name("Test2")
            .email("test2@email.com")
            .teamId(1L)
            .build());

        persistAndReturnEntity(Player.builder()
            .name("Test3")
            .email("test3@email.com")
            .teamId(2L)
            .build());

        List<Player> teamPlayers = playerService.getPlayersByTeam(1L);

        assertEquals(2, teamPlayers.size());
        assertTrue(teamPlayers.stream().anyMatch(p -> p.getName().equals("Test1")));
        assertTrue(teamPlayers.stream().anyMatch(p -> p.getName().equals("Test2")));
        assertFalse(teamPlayers.stream().anyMatch(p -> p.getName().equals("Test3")));
    }

    @Test
    void testGetPlayersByTeam_NoPlayers() {
        List<Player> teamPlayers = playerService.getPlayersByTeam(999L);

        assertTrue(teamPlayers.isEmpty());
    }
}