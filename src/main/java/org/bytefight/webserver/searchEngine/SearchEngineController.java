package org.bytefight.webserver.searchEngine;

import lombok.RequiredArgsConstructor;

import org.bytefight.webserver.team.domain.dto.PublicTeamDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/search")
public class SearchEngineController {
  private static final int DEFAULT_PAGE_SIZE = 24;
  private static final int MAX_PAGE_SIZE = 100;

  private final SearchEngineService searchEngineService;

  @GetMapping("/team")
  public ResponseEntity<Page<PublicTeamDto>> searchTeam(
      @RequestParam String searchParam,
      @RequestParam String competitionSlug,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
    int pageIndex = Math.max(page, 0);
    int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    Pageable pageable = PageRequest.of(pageIndex, pageSize);
    Page<PublicTeamDto> teamSearchResult =
        searchEngineService.searchRecruitingTeams(searchParam, competitionSlug, pageable);
    return ResponseEntity.ok(teamSearchResult);
  }
}
