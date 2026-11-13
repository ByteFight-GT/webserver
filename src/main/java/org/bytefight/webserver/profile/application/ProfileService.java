package org.bytefight.webserver.profile.application;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.bytefight.webserver.competition.application.CompetitionAccessGuard;
import org.bytefight.webserver.leaderboard.domain.MemberSummaryDto;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.player.infra.PlayerRepository;
import org.bytefight.webserver.profile.domain.Profile;
import org.bytefight.webserver.profile.domain.ProfileDto;
import org.bytefight.webserver.profile.domain.ProfileRequest;
import org.bytefight.webserver.profile.domain.SuggestedProfileDto;
import org.bytefight.webserver.profile.domain.dto.PlayerCompetitionDto;
import org.bytefight.webserver.profile.infra.ProfileRepository;
import org.bytefight.webserver.team.domain.Team;
import org.bytefight.webserver.team.domain.TeamMember;
import org.bytefight.webserver.team.infra.TeamMemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ProfileService {
  private final CompetitionAccessGuard competitionAccessGuard;
  private final TeamMemberRepository teamMemberRepository;
  private final ProfileRepository profileRepository;
  private final PlayerRepository playerRepository;

  public List<PlayerCompetitionDto> getCompetitionsByPlayer(Player player) {
    List<TeamMember> memberships = teamMemberRepository.findByPlayerAndTeamDeletedAtNull(player);

    return memberships.stream()
        .map(
            membership -> {
              Team team = membership.getTeam();
              var competition = team.getCompetition();

              if (!competitionAccessGuard.canAccess(competition)) {
                return null;
              }

              List<MemberSummaryDto> memberDtos =
                  teamMemberRepository.findByTeam(team).stream()
                      .map(TeamMember::getPlayer)
                      .map(
                          p ->
                              new MemberSummaryDto(
                                  p.getUser().getUuid().toString(), p.getUsername(), p.isDev()))
                      .toList();

              return new PlayerCompetitionDto(
                  competition.getSlug(),
                  competition.getName(),
                  team.getName(),
                  team.getUuid().toString(),
                  team.isDisplayMembers() ? memberDtos : null,
                  team.getCachedLeaderboardRank());
            })
        .filter(Objects::nonNull)
        .toList();
  }

  @Transactional
  public ProfileDto createProfile(Player player, ProfileRequest request) {
    validateRequest(request);
    if (profileRepository.findByPlayerId(player.getId()).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Profile already exists");
    }

    Profile profile = new Profile();
    profile.setPlayer(
        playerRepository
            .findByIdWithUser(player.getId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found")));
    applyRequest(profile, request);
    return ProfileDto.from(profileRepository.save(profile));
  }

  @Transactional
  public ProfileDto getProfile(Player player) {
    return ProfileDto.from(
        profileRepository
            .findByPlayerId(player.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
  }

  @Transactional
  public ProfileDto updateProfile(Player player, ProfileRequest request) {
    validateRequest(request);
    Profile profile =
        profileRepository
            .findByPlayerId(player.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    applyRequest(profile, request);
    return ProfileDto.from(profileRepository.save(profile));
  }

  @Transactional
  public void deleteProfile(Player player) {
    Profile profile =
        profileRepository
            .findByPlayerId(player.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    profileRepository.delete(profile);
  }

  @Transactional
  public Page<ProfileDto> searchProfiles(
      String query, String major, String school, Integer graduationYear, Pageable pageable) {
    return profileRepository
        .searchProfiles(
            normalize(query), normalize(major), normalize(school), graduationYear, pageable)
        .map(ProfileDto::from);
  }

  @Transactional
  public Optional<ProfileDto> findPublicProfile(UUID playerUuid) {
    return profileRepository.findByPlayerUuid(playerUuid).map(ProfileDto::from);
  }

  @Transactional
  public Page<SuggestedProfileDto> getSuggestions(Player player, Pageable pageable) {
    Profile currentProfile =
        profileRepository
            .findByPlayerId(player.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    Page<Profile> suggestions =
        profileRepository.findSuggestions(
            player.getId(),
            currentProfile.getMajor(),
            currentProfile.getSchool(),
            currentProfile.getGraduationYear(),
            pageable);

    List<SuggestedProfileDto> content =
        suggestions.getContent().stream()
            .map(
                candidate ->
                    SuggestedProfileDto.builder()
                        .profile(ProfileDto.from(candidate))
                        .matchingFields(matchingFields(currentProfile, candidate))
                        .build())
            .toList();
    return new PageImpl<>(content, suggestions.getPageable(), suggestions.getTotalElements());
  }

  private void validateRequest(ProfileRequest request) {
    if (request == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profile details are required");
    }
    if (normalize(request.getDescription()) == null
        && normalize(request.getMajor()) == null
        && normalize(request.getSchool()) == null
        && request.getGraduationYear() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "At least one profile detail must be provided");
    }
  }

  private void applyRequest(Profile profile, ProfileRequest request) {
    profile.setDescription(normalize(request.getDescription()));
    profile.setMajor(normalize(request.getMajor()));
    profile.setSchool(normalize(request.getSchool()));
    profile.setGraduationYear(request.getGraduationYear());
  }

  private List<String> matchingFields(Profile current, Profile candidate) {
    List<String> fields = new ArrayList<>();
    if (sameText(current.getMajor(), candidate.getMajor())) {
      fields.add("major");
    }
    if (sameText(current.getSchool(), candidate.getSchool())) {
      fields.add("school");
    }
    if (current.getGraduationYear() != null
        && current.getGraduationYear().equals(candidate.getGraduationYear())) {
      fields.add("graduationYear");
    }
    return fields;
  }

  private boolean sameText(String first, String second) {
    return first != null
        && second != null
        && first.trim().toLowerCase(Locale.ROOT).equals(second.trim().toLowerCase(Locale.ROOT));
  }

  private String normalize(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
