package com.example.botfightwebserver.gameMatch.application;

import com.example.botfightwebserver.gameMatch.domain.GameMatch;
import com.example.botfightwebserver.gameMatch.domain.GameMatchDto;
import com.example.botfightwebserver.gameMatch.domain.MATCH_REASON;
import com.example.botfightwebserver.gameMatch.infra.GameMatchRepository;
import com.example.botfightwebserver.team.application.TeamService;
import com.example.botfightwebserver.team.domain.Team;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameMatchSearchService {
    private final EntityManager entityManager;
    private final GameMatchService gameMatchService;
    private final GameMatchRepository gameMatchRepository;
    private final ConversionService conversionService;
    private final TeamService teamService;
    private SearchSession searchSession;

    @PostConstruct
    public void init() {
        searchSession = Search.session(entityManager);
    }

    private Optional<String> resolveOpponentTeamUuid(Optional<String> teamSearchparam) {
        if (teamSearchparam.isEmpty()) {
            return Optional.empty();
        }

        SearchResult<Team> result = searchSession.search(Team.class)
                .where(f -> f.match()
                        .field("name")
                        .matching(teamSearchparam.get())
                        .fuzzy(2))
                .fetch(0, 1);

        if (result.hits().isEmpty()) {
            return Optional.empty();
        }

        String teamUuid = result.hits().get(0).getUuid().toString();
        return Optional.of(teamUuid);
    }

    private Specification<GameMatch> hasTeam(UUID teamUuid) {
        return (root, query, cb) ->
                cb.or(
                        cb.equal(root.get("teamOne").get("uuid"), teamUuid),
                        cb.equal(root.get("teamTwo").get("uuid"), teamUuid)
                );
    }

    private Specification<GameMatch> hasOpponentTeam(UUID opponentUuid) {
        return (root, query, cb) ->
                cb.or(
                        cb.equal(root.get("teamOne").get("uuid"), opponentUuid),
                        cb.equal(root.get("teamTwo").get("uuid"), opponentUuid)
                );
    }

    private Specification<GameMatch> hasReason(MATCH_REASON reason) {
        return (root, query, cb) -> cb.equal(root.get("reason"), reason);
    }

    private Specification<GameMatch> hasMap(String map) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("map")), map.toLowerCase());
    }

    public Page<GameMatchDto> searchGame(Optional<String> teamSearchparam,
                                         Optional<String> requiredTeamUuid,
                                         Optional<MATCH_REASON> reason,
                                         Pageable pageable) {
        String teamUuid = requiredTeamUuid
                .orElseThrow(() -> new IllegalArgumentException("requiredTeamUuid is required"));

        Optional<String> opponentUuid = resolveOpponentTeamUuid(teamSearchparam);

        Specification<GameMatch> spec = Specification.allOf(
                hasTeam(UUID.fromString(teamUuid)),
                opponentUuid.map(uuid -> hasOpponentTeam(UUID.fromString(uuid))).orElse(null),
                reason.map(this::hasReason).orElse(null)
        );

        Sort sort = Sort.by(
                Sort.Order.desc("processedAt")
        );

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort
        );

        Page<GameMatch> page = gameMatchRepository.findAll(spec, sortedPageable);

        return page.map(GameMatchDto::fromEntity);
    }
}
