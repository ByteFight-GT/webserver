package com.example.botfightwebserver.gameMatch.domain;

import com.example.botfightwebserver.common.domain.BaseEntity;
import com.example.botfightwebserver.team.domain.Team;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "game_match_files",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_game_match_files_match_slug_team",
                        columnNames = {"game_match_id", "slug", "team_id"}
                )
        }
)
public class GameMatchFile extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_match_id", nullable = false)
    private GameMatch gameMatch;

    @Column(name = "uuid", nullable = false)
    private UUID uuid;

    @Column(name = "slug", nullable = false, length = 50)
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_record_id", nullable = false)
    private com.example.botfightwebserver.storage.domain.FileRecord fileRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;
}
