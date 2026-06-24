package org.bytefight.webserver.player.domain;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bytefight.webserver.common.domain.BaseEntity;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "player_social_links")
public class SocialLink extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 30)
    private SocialPlatform platform;

    @Column(name = "url", nullable = false, length = 500)
    private String url;
}