package com.example.botfightwebserver.searchEngine;

import com.example.botfightwebserver.gameMatch.GameMatchDTO;
import com.example.botfightwebserver.gameMatch.GameMatchService;
import com.example.botfightwebserver.gameMatch.MATCH_REASON;
import com.example.botfightwebserver.team.Team;
import com.example.botfightwebserver.team.TeamService;
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
import java.util.Optional;
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

    public Page<Team> searchTeamByNameFuzzy(String searchTerm, Pageable pageable) {
        log.info("Searching teams with fuzzy match for name: {}", searchTerm);

        SearchResult<Team> result = searchSession.search(Team.class)
            .where(f -> f.match()
                .field("name")
                .matching(searchTerm)
                .fuzzy(2))
            .fetch((int) pageable.getOffset(), pageable.getPageSize());

        return new PageImpl<>(
            result.hits(),
            pageable,
            result.total().hitCount()
        );
    }

    public Page<GameMatchDTO> searchGame(Optional<String> teamSearchparam,
                                         Optional<Long> requiredTeamId,
                                         Optional<MATCH_REASON> reason,
                                         Optional<String> map,
                                         Pageable pageable) {

        Page<GameMatchDTO> results;
        if (teamSearchparam.isPresent()) {
            SearchResult<Team> result = searchSession.search(Team.class)
                .where(f -> f.match()
                    .field("name")
                    .matching(teamSearchparam.get())
                    .fuzzy(2)).fetch(0, 1);
            results = gameMatchService.getPlayedTeamMatches(result.hits().get(0).getId(), requiredTeamId.get(),  pageable.getPageNumber(),
                pageable.getPageSize());
        } else {
            results = gameMatchService.getPlayedTeamMatches(requiredTeamId.get(),
                pageable.getPageNumber(),
                pageable.getPageSize());
        }

        List<GameMatchDTO> filteredContent = results.getContent();

        if (reason.isPresent()) {
            filteredContent = filteredContent.stream()
                .filter(gameMatchDTO -> reason.get().equals(gameMatchDTO.getReason()))
                .collect(Collectors.toList());
        }

        if (map.isPresent() && !map.get().isEmpty()) {
            filteredContent = filteredContent.stream()
                .filter(gameMatchDTO -> map.get().equalsIgnoreCase(gameMatchDTO.getMap()))
                .collect(Collectors.toList());
        }

        if (reason.isPresent() || (map.isPresent() && !map.get().isEmpty())) {
            results = new PageImpl<>(
                filteredContent,
                results.getPageable(),
                filteredContent.size()
            );
        }

        return results;

    }

}
