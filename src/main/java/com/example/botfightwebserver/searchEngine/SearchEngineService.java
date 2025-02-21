package com.example.botfightwebserver.searchEngine;

import com.example.botfightwebserver.gameMatch.GameMatchDTO;
import com.example.botfightwebserver.gameMatch.GameMatchService;
import com.example.botfightwebserver.team.Team;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchEngineService {

    private final EntityManager entityManager;
    private final GameMatchService gameMatchService;
    private SearchSession searchSession;

    @PostConstruct
    public void init() {
        searchSession = Search.session(entityManager);
    }

    // may not be efficient due to deep page problem but works for now.
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

    public Page<GameMatchDTO> searchGame(String teamSearchparam,
                                         Long requiredTeamId,
                                         Pageable pageable) {
        SearchResult<Team> result = searchSession.search(Team.class)
            .where(f -> f.match()
                .field("name")
                .matching(teamSearchparam)
                .fuzzy(2))
            .fetch(0, 1);

        System.out.println(result.hits());
        System.out.println(result.total().hitCount());
        System.out.println(result.hits().get(0).getName());
        if (result.hits().isEmpty()) {
            return Page.empty();
        }

        return gameMatchService.getPlayedTeamMatches(result.hits().get(0).getId(), requiredTeamId,  pageable.getPageNumber(),
            pageable.getPageSize());
    }

}
