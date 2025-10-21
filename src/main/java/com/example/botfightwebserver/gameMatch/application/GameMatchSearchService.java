package com.example.botfightwebserver.gameMatch.application;

import com.example.botfightwebserver.gameMatch.domain.GameMatchDTO;
import com.example.botfightwebserver.gameMatch.domain.MATCH_REASON;
import com.example.botfightwebserver.team.application.TeamService;
import com.example.botfightwebserver.team.domain.Team;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
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
public class GameMatchSearchService {
    private final EntityManager entityManager;
    private final GameMatchService gameMatchService;
    private final ConversionService conversionService;
    private final TeamService teamService;
    private SearchSession searchSession;

    @PostConstruct
    public void init() {
        searchSession = Search.session(entityManager);
    }

    public Page<GameMatchDTO> searchGame(Optional<String> teamSearchparam,
                                         Optional<String> requiredTeamUuid,
                                         Optional<MATCH_REASON> reason,
                                         Optional<String> map,
                                         Pageable pageable) {
        List<GameMatchDTO> allMatches = gameMatchService.getAllTeamMatches(requiredTeamUuid.get());
        if (teamSearchparam.isPresent()) {
            SearchResult<Team> result = searchSession.search(Team.class)
                    .where(f -> f.match()
                            .field("name")
                            .matching(teamSearchparam.get())
                            .fuzzy(2)).fetch(0, 1);
            if (!result.hits().isEmpty()) {
                String teamUuid = result.hits().get(0).getUuid().toString();
                allMatches = allMatches.stream()
                        .filter(gameMatchDTO ->
                                teamUuid.equals(gameMatchDTO.getTeamOneUuid()) ||
                                        teamUuid.equals(gameMatchDTO.getTeamTwoUuid()))
                        .toList();
            } else {
                allMatches = List.of();
            }
        }

        if (reason.isPresent()) {
            allMatches = allMatches.stream()
                    .filter(gameMatchDTO -> reason.get().equals(gameMatchDTO.getReason()))
                    .collect(Collectors.toList());
        }

        if (map.isPresent() && !map.get().isEmpty()) {
            allMatches = allMatches.stream()
                    .filter(gameMatchDTO -> map.get().equalsIgnoreCase(gameMatchDTO.getMap()))
                    .collect(Collectors.toList());
        }

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allMatches.size());

        List<GameMatchDTO> pagedContent = start < end ?
                allMatches.subList(start, end) :
                List.of();

        return new PageImpl<>(
                pagedContent,
                pageable,
                allMatches.size()
        );
    }
}
