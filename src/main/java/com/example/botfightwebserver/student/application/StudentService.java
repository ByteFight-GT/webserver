package com.example.botfightwebserver.student.application;

import com.example.botfightwebserver.player.application.PlayerService;
import com.example.botfightwebserver.student.domain.StudentDto;
import com.example.botfightwebserver.student.domain.StudentEmail;
import com.example.botfightwebserver.team.domain.Team;
import com.example.botfightwebserver.team.infra.TeamRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class StudentService {
    private final StudentEmailRepository studentEmailRepository;
    private final PlayerService playerService;
    private final TeamRepository teamRepository;

    public List<StudentDto> list() {
        List<Team> teams = teamRepository.findAllByOrderByGlickoDescMatchesPlayedAscIdAsc();
        Map<UUID, Integer> teamToRank = new HashMap<>();
        AtomicInteger rank = new AtomicInteger(1);

        teams.forEach((t) -> {
            teamToRank.put(t.getUuid(), rank.getAndIncrement());
        });

        Set<String> emails = studentEmailRepository.findAll().stream().map(StudentEmail::getEmail).collect(Collectors.toSet());

        return playerService.getPlayers().stream().filter(p -> emails.contains(p.getUser().getEmail())).map(p -> {
            var builder = StudentDto.builder()
                    .id(p.getUser().getUuid().toString())
                    .email(p.getUser().getEmail())
                    .playerName(p.getName());

            if (p.getTeam() != null) {
                builder = builder.teamUuid(p.getTeam().getUuid().toString())
                        .teamName(p.getTeam().getName())
                        .teamGlicko(p.getTeam().getGlicko())
                        .teamRanking(teamToRank.getOrDefault(p.getTeam().getUuid(), -1));
            }

            return builder.build();
        }).toList();
    }
}
