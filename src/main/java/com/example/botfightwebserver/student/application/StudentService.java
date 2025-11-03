package com.example.botfightwebserver.student.application;

import com.example.botfightwebserver.player.application.PlayerService;
import com.example.botfightwebserver.student.domain.StudentDto;
import com.example.botfightwebserver.team.domain.Team;
import com.example.botfightwebserver.team.infra.TeamRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Transactional
@RequiredArgsConstructor
public class StudentService {
    private final PlayerService playerService;
    private final TeamRepository teamRepository;

    public List<StudentDto> list() {
        List<Team> teams = teamRepository.findAllByOrderByGlickoDescMatchesPlayedAscIdAsc();
        Map<UUID, Integer> teamToRank = new HashMap<>();
        AtomicInteger rank = new AtomicInteger(1);

        teams.forEach((t) -> {
            teamToRank.put(t.getUuid(), rank.getAndIncrement());
        });

        return playerService.getPlayers().stream().filter(p -> p.getTeam() != null).map(p ->
                StudentDto.builder()
                        .id(p.getUser().getUuid().toString())
                        .email(p.getUser().getEmail())
                        .playerName(p.getName())
                        .teamUuid(p.getTeam().getUuid().toString())
                        .teamName(p.getTeam().getName())
                        .teamGlicko(p.getTeam().getGlicko())
                        .teamRanking(teamToRank.getOrDefault(p.getTeam().getUuid(), -1))
                        .build()
        ).toList();
    }
}
