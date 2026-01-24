package com.example.botfightwebserver.matchMaking.domain;

import com.example.botfightwebserver.common.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@Entity
@Table(name = "matchmaking_events")
public class MatchmakingEvent extends BaseEntity {

}
