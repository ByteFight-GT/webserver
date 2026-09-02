package org.bytefight.webserver.leaderboard.domain;

import lombok.Value;

@Value
public class MemberSummaryDto {
  String uuid;
  String username;
  boolean isDev;

  public MemberSummaryDto(String uuid, String username, boolean isDev) {
    this.uuid = uuid;
    this.username = username;
    this.isDev = isDev;
  }
}
