package org.bytefight.webserver.gamematch.domain.dto;

import java.util.List;

public record AdminRescheduleResultDto(List<Long> rescheduled, List<Skipped> skipped) {
  public record Skipped(Long id, String reason) {}
}
