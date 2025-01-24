package com.example.botfightwebserver.player;

import com.example.botfightwebserver.submission.Submission;
import com.example.botfightwebserver.team.Team;
import com.google.common.annotations.VisibleForTesting;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    @Column(nullable = false, unique = true)
    private UUID authId;

    private String name;

    private String email;

    private Long teamId;

    private boolean hasTeam;

    @Builder.Default
    @Column(nullable = false)
    private Integer badgeBitFlags = 0;

    private LocalDateTime creationDateTime;

    private static Clock clock = Clock.systemDefaultZone();

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
