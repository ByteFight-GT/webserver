package org.bytefight.webserver.globalstats.infra;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.Map;

import org.bytefight.webserver.globalstats.application.GlobalStatsService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Global Stats (Public)", description = "Public platform-wide statistics")
@RestController
@RequestMapping("/api/v1/public/global-stats")
@RequiredArgsConstructor
public class PublicGlobalStatsController {
  private final GlobalStatsService globalStatsService;

  /**
   * Deliberately returns a bare map instead of a DTO: the set of keys grows as new StatCollectors
   * are added, so clients must not depend on a fixed shape.
   */
  @GetMapping
  @Operation(
      operationId = "getGlobalStats",
      summary = "Get all global stats as a metric-to-value dictionary")
  public ResponseEntity<Map<String, Long>> getGlobalStats() {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic())
        .body(globalStatsService.getAllStats());
  }
}
