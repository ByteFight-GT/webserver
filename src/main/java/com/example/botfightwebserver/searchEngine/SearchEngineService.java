package com.example.botfightwebserver.searchEngine;

import com.example.botfightwebserver.gameMatch.GameMatch;
import com.example.botfightwebserver.gameMatch.GameMatchDTO;
import com.example.botfightwebserver.gameMatch.MATCH_REASON;
import com.example.botfightwebserver.gameMatch.MATCH_STATUS;
import com.example.botfightwebserver.team.Team;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchEngineService {

    private final EntityManager entityManager;
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
                                         String requiredTeamName,
                                         MATCH_STATUS matchStatus,
                                         MATCH_REASON matchReason,
                                         Pageable pageable) {

        SearchResult<GameMatch> result = searchSession.search(GameMatch.class)
            .where(f -> {
                BooleanPredicateClausesStep<?> bool = f.bool();

                if (StringUtils.hasText(teamSearchparam)) {
                    bool.must(f.match()
                        .field("team")
                        .matching(teamSearchparam));
                }

                if (StringUtils.hasText(requiredTeamName)) {
                    bool.must(f.match()
                        .field("requiredTeam")
                        .matching(requiredTeamName));
                }

                if (matchStatus != null) {
                    bool.must(f.match()
                        .field("matchStatus")
                        .matching(matchStatus));
                }

                if (matchReason != null) {
                    bool.must(f.match()
                        .field("matchReason")
                        .matching(matchReason));
                }

                return bool;
            })
            .fetch((int) pageable.getOffset(), pageable.getPageSize());

        return new PageImpl<>(
            result.hits().stream()
                .map(GameMatchDTO::fromEntity)
                .collect(Collectors.toList()),
            pageable,
            result.total().hitCount()
        );
    }

}
