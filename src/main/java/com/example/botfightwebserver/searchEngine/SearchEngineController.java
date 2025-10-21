package com.example.botfightwebserver.searchEngine;

import com.example.botfightwebserver.gameMatch.domain.GameMatchDTO;
import com.example.botfightwebserver.gameMatch.domain.MATCH_REASON;
import com.example.botfightwebserver.team.domain.PublicTeamDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/search")
public class SearchEngineController {

    private final SearchEngineService searchEngineService;

    @GetMapping("/team")
    public ResponseEntity<Page<PublicTeamDto>> searchTeam(@RequestParam String searchParam,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        System.out.println("searchTeam");
        Page<PublicTeamDto> teamSearchResult = searchEngineService.searchTeamByNameFuzzy(searchParam, pageable)
            .map(PublicTeamDto::from);
        return ResponseEntity.ok(teamSearchResult);
    }


}
