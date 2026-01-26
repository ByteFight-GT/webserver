package org.bytefight.webserver.gameMatch.application;

import org.bytefight.webserver.gameMatch.domain.dto.AdminGameMatchDto;
import org.bytefight.webserver.gameMatch.domain.GameMatch;
import org.bytefight.webserver.gameMatch.infra.GameMatchRepository;
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

    public Page<AdminGameMatchDto> list(Specification<GameMatch> specs, Pageable pageable) {
        return gameMatchRepository.findAll(specs, pageable).map(AdminGameMatchDto::fromEntity);
    }
}
