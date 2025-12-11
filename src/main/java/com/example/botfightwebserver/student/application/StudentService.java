package com.example.botfightwebserver.student.application;

import com.example.botfightwebserver.player.application.PlayerService;
import com.example.botfightwebserver.player.domain.Player;
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

    public List<StudentDto> list() {
        List<String> emails = studentEmailRepository.findAll().stream().map(StudentEmail::getEmail).collect(Collectors.toList());

        Map<String, Player> emailToPlayer = playerService.getPlayers().stream().collect(Collectors.toMap(
                player -> player.getUser().getEmail(),
                player -> player
        ));

        return emails.stream().map(email -> {
            Player p = emailToPlayer.get(email);

            var builder = StudentDto.builder()
                    .email(email);

            if(p != null) {
                builder = builder
                        .playerName(p.getUsername())
                        .id(p.getUser().getUuid().toString());

                if (p.getTeam() != null) {
                    builder = builder
                            .teamUuid(p.getTeam().getUuid().toString())
                            .teamName(p.getTeam().getName())
//                            .teamGlicko(p.getTeam().getGlicko())
                            .teamRanking(-1);
                }
            }

            return builder.build();
        }).toList();
    }
}
