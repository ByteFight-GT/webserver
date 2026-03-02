package org.bytefight.webserver.leaderboard.domain;

import lombok.Value;

@Value
public class MemberSummaryDto {
  String uuid;
  String username;

  public MemberSummaryDto(String uuid, String username) {
    this.uuid = uuid;
    this.username = username;
  }
}
