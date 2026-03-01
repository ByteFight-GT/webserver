package org.bytefight.webserver.competition.domain.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class JoinTeamDto {
  String joinCode;
}
