package org.bytefight.webserver.gamematchfile.domain;

import org.bytefight.webserver.common.domain.BaseEntity;
import org.bytefight.webserver.gamematch.domain.GameMatch;
import org.bytefight.webserver.gamematch.domain.MatchStatus;
import org.bytefight.webserver.team.domain.Team;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bytefight.webserver.storage.domain.FileRecord;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    private FileRecord fileRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "visibility", nullable = false, columnDefinition = "game_match_file_visibility")
    private GameMatchFileVisibility visibility;
}
