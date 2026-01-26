package org.bytefight.webserver.searchEngine;

import org.bytefight.webserver.gameMatch.application.GameMatchService;
import org.bytefight.webserver.team.domain.dto.PublicTeamDto;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.application.TeamService;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchEngineService {

    private final EntityManager entityManager;
    private final GameMatchService gameMatchService;
    private final ConversionService conversionService;
    private final TeamService teamService;
    private SearchSession searchSession;

    @PostConstruct
    public void init() {
        searchSession = Search.session(entityManager);
    }

    public Page<PublicTeamDto> searchTeamByNameFuzzy(String searchTerm, Pageable pageable) {
        log.info("Searching teams with fuzzy match for name: {}", searchTerm);

        SearchResult<Team> result = searchSession.search(Team.class)
            .where(f -> f.match()
                .field("name")
                .matching(searchTerm)
                .fuzzy(2))
            .fetch((int) pageable.getOffset(), pageable.getPageSize());

        List<PublicTeamDto> dtos = result.hits().stream()
                .map(team -> {
                    return PublicTeamDto.from(team, List.of());
                })
                .collect(Collectors.toList());

        return new PageImpl<>(
            dtos,
            pageable,
            result.total().hitCount()
        );
    }
}
