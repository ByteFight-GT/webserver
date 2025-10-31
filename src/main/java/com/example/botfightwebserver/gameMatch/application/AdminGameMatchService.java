package com.example.botfightwebserver.gameMatch.application;

import com.example.botfightwebserver.gameMatch.domain.GameMatch;
import com.example.botfightwebserver.gameMatch.domain.GameMatchDTO;
import com.example.botfightwebserver.gameMatch.infra.GameMatchRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminGameMatchService {
    private final GameMatchRepository gameMatchRepository;

    public Page<GameMatchDTO> list(Specification<GameMatch> specs, Pageable pageable) {
        return gameMatchRepository.findAll(specs, pageable).map(GameMatchDTO::fromEntity);
    }
}
