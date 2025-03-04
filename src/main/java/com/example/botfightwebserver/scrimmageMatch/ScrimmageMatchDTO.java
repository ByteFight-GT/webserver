package com.example.botfightwebserver.scrimmageMatch;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScrimmageMatchDTO {
    private Long id;
    private Long matchId;
    private Long teamId;

    public static ScrimmageMatchDTO fromEntity(ScrimmageMatch scrimmageMatch) {
        ScrimmageMatchDTO dto = ScrimmageMatchDTO.builder()
            .id(scrimmageMatch.getId())
            .matchId(scrimmageMatch.getId())
            .teamId(scrimmageMatch.getInitiatorTeam().getId())
            .build();
        return dto;
    }
}
