package com.example.botfightwebserver.player.domain;

import com.example.botfightwebserver.auth.domain.User;
import com.example.botfightwebserver.common.domain.BaseEntity;
import com.example.botfightwebserver.team.domain.Team;
import com.google.api.gax.rpc.UnimplementedException;
import com.google.common.annotations.VisibleForTesting;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.checkerframework.common.aliasing.qual.Unique;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "players")
public class Player extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "username_normalized", nullable = false, unique = true, length = 50)
    private String usernameNormalized;

    public void setTeam(Team team) {
        throw new RuntimeException("This method is deprecated");
    }

    public Team getTeam() {
        throw new RuntimeException("This method is deprecated");
    }
}
