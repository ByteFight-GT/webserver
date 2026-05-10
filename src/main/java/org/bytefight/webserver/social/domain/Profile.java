package org.bytefight.webserver.social.domain;

import org.bytefight.webserver.auth.domain.User;
import org.bytefight.webserver.common.domain.AuditableSoftDeletableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bytefight.webserver.player.domain.Player;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "Profile")
public class Profile extends AuditableSoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "major", length=256, nullable = false)
    private String major;

    @Column(name = "year", length = 50, nullable = false)
    private Integer year;

}
