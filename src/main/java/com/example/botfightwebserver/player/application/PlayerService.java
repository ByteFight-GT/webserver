package com.example.botfightwebserver.player.application;
;
import com.example.botfightwebserver.auth.domain.User;
import com.example.botfightwebserver.permissions.application.PermissionsService;
import com.example.botfightwebserver.player.domain.Player;
import com.example.botfightwebserver.player.infra.PlayerRepository;
import com.example.botfightwebserver.student.application.StudentEmailRepository;
import com.example.botfightwebserver.team.domain.Team;
import com.example.botfightwebserver.team.infra.TeamRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final PermissionsService permissionsService;
    private final TeamRepository teamRepository;
    private final StudentEmailRepository studentEmailRepository;

    public List<Player> getPlayers() {
        return playerRepository.findAll()
            .stream()
            .toList();
    }

    public Player createPlayer(User user, String name, Long teamId) {
        Player player = new Player();
        player.setName(name);
        player.setTeam(null);
        player.setUser(user);
        return playerRepository.save(player);
    }

    public void setName(Long playerId, String name) {
        permissionsService.validateAllowUpdateProfile();
        if (!playerRepository.existsById(playerId)) {
            throw new IllegalArgumentException("Player with id " + playerId + " does not exist");
        }
        Player player = playerRepository.findById(playerId).get();
        player.setName(name);
        playerRepository.save(player);
    }

    public Player setPlayerTeam(UUID playerId, Team team) {
        permissionsService.validateAllowJoinTeam();

        if (!playerRepository.existsByUserUuid(playerId)) {
            throw new IllegalArgumentException("Player with id " + playerId + " does not exist");
        }
        Player player = playerRepository.findByUserUuid(playerId).orElse(null);

        if(permissionsService.get().getRestrictTeamCreationToStudentEmails()) {
            if(!studentEmailRepository.existsByEmail(player.getUser().getEmail())) {
                throw new IllegalArgumentException("You are not whitelisted for this competition.");
            }
        }

        player.setTeam(team);
        player.setHasTeam(true);
        team.setNumberPlayers(team.getNumberPlayers() + 1);
        teamRepository.save(team);
        return playerRepository.save(player);
    }

    @Transactional
    public Team leaveTeam(Player player) {
        Team oldTeam = player.getTeam();
        player.setHasTeam(false);
        player.setTeam(null);
        oldTeam.setNumberPlayers(oldTeam.getNumberPlayers() - 1);
        teamRepository.save(oldTeam);
        playerRepository.save(player);
        return oldTeam;
    }

    public List<Player> getPlayersByTeam(Long teamId) {
        return playerRepository.findByTeamId(teamId);
    }

    public Player getPlayer(Long playerId) {
        if (playerId == null) {
            throw new IllegalArgumentException("Player id cannot be null");
        }
        return playerRepository.findById(playerId).orElseThrow(() -> new IllegalArgumentException("Player not found"));
    }

    public Player getPlayer(UUID authId) {
        if (authId == null) {
            throw new IllegalArgumentException("Auth id cannot be null");
        }
        return playerRepository.findByUserUuid(authId).orElseThrow(() -> new IllegalArgumentException("Player not found"));
    }

    public Player getPlayer(User user) {
        return playerRepository.findByUser(user).orElseThrow(() -> new IllegalArgumentException("Player not found"));
    }

    public boolean isUsernameExist(String username) {
        System.out.println(username);
        return playerRepository.existsByName(username);
    }

    public boolean isEmailExist(String email) {
        return playerRepository.existsByUserEmail(email);
    }

    public Team getTeamFromUUID(UUID uuid) {
        Player player = getPlayer(uuid);
        if (!player.isHasTeam()) {
            throw new IllegalArgumentException("Player with UUID " + uuid + " has no team");
        }
        return player.getTeam();
    }

    public Long getNumberPlayers() {
        return playerRepository.count();
    }


}
