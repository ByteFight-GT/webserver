package com.example.botfightwebserver.team.domain;

import lombok.Value;

@Value
public class EditTeamDto {
    String name;
    String quote;
    boolean displayMembers = false;
}
