package com.example.botfightwebserver.player;

import com.example.botfightwebserver.auth.domain.User;
import com.google.common.annotations.VisibleForTesting;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.checkerframework.common.aliasing.qual.Unique;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Unique
    private String name;

    private Long teamId;

    private boolean hasTeam;

    @Builder.Default
    @Column(nullable = false)
    private Integer badgeBitFlags = 0;

    private LocalDateTime creationDateTime;

    private static Clock clock = Clock.system(ZoneId.of("America/New_York"));

    @PrePersist
    public void onCreate() {
        creationDateTime = LocalDateTime.now(clock);
    }

    @VisibleForTesting
    public static void setClock(Clock testClock) {
        clock = testClock;
    }

    public boolean hasBadge(Badge badge) {
        return (badgeBitFlags & badge.getBitFlag()) != 0;
    }

    public void addBadge(Badge badge) {
        badgeBitFlags |= badge.getBitFlag();
    }

    public void removeBadge(Badge badge) {
        badgeBitFlags &= ~badge.getBitFlag();
    }

    public List<String> getBadgeList() {
        List<String> badges = new ArrayList<>();
        for (Badge badge : Badge.values()) {
            if (hasBadge(badge)) {
                badges.add(badge.getDisplayName());
            }
        }
        return badges;
    }
}
