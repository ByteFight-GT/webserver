package com.example.botfightwebserver.team.infra;

import com.example.botfightwebserver.competition.domain.Competition;
import com.example.botfightwebserver.player.domain.Player;
import com.example.botfightwebserver.team.domain.Team;
import com.example.botfightwebserver.team.domain.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    boolean existsByCompetitionAndPlayer(Competition competition, Player player);
    Optional<TeamMember> findByTeamAndPlayer(Team team, Player player);
}
