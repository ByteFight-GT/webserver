package com.example.botfightwebserver.student.application;

import com.example.botfightwebserver.gameMatch.domain.GameMatch;
import com.example.botfightwebserver.gameMatch.domain.GameMatchDTO;
import com.example.botfightwebserver.player.application.PlayerService;
import com.example.botfightwebserver.player.infra.PlayerRepository;
import com.example.botfightwebserver.student.domain.StudentDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class StudentService {
    private final PlayerService playerService;

    public List<StudentDto> list() {
        return playerService.getPlayers().stream().filter(p -> p.getTeam() != null).map(p ->
                StudentDto.builder()
                        .id(p.getUser().getUuid().toString())
                        .email(p.getUser().getEmail())
                        .playerName(p.getName())
                        .teamUuid(p.getTeam().getUuid().toString())
                        .teamName(p.getTeam().getName())
                        .teamGlicko(p.getTeam().getGlicko())
                        .build()
        ).toList();
    }
}
