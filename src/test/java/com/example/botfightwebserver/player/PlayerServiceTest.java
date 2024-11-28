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

    @MockBean
    private TeamService teamService;

    private PlayerService playerService;

    @BeforeEach
    void setUp() {
        playerService = new PlayerService(playerRepository, teamService);
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
        when(teamService.isExistById(1L)).thenReturn(true);

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
    void testCreatePlayer_TeamDoesNotExist() {
        when(teamService.isExistById(1L)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> playerService.createPlayer("Test", "test@email.com", 1L)
        );

        assertEquals("Team with id 1 does not exist", exception.getMessage());
    }

    @Test
    void testSetPlayerTeam() {
        Player player = persistAndReturnEntity(Player.builder()
            .name("Test")
            .build());

        when(teamService.isExistById(2L)).thenReturn(true);

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
}