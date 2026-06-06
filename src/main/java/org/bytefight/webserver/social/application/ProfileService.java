package org.bytefight.webserver.social.application;

import lombok.RequiredArgsConstructor;
import org.bytefight.webserver.player.domain.Player;
import org.bytefight.webserver.social.domain.Profile;
import org.bytefight.webserver.social.infra.ProfileSpecification;
import org.bytefight.webserver.social.domain.dto.PublicProfileDto;
import org.bytefight.webserver.social.infra.ProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final ProfileRepository profileRepository;

    @Transactional(readOnly = true)
    public List<PublicProfileDto> getProfiles(String username, String major, Integer year, String keyword) {
        return profileRepository.findAll(ProfileSpecification.fromFilter(username, major, year, keyword))
                .stream()
                .map(PublicProfileDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<PublicProfileDto> getProfiles(String username, String major, Integer year, String keyword, Pageable pageable) {
        return profileRepository.findAll(ProfileSpecification.fromFilter(username, major, year, keyword), pageable)
                .map(PublicProfileDto::from);
    }

    @Transactional
    public PublicProfileDto createProfile(Player player, String description, String major, Integer year) {
        if (profileRepository.existsByPlayerAndIsDeletedFalse(player)) {
            throw new IllegalArgumentException("Player already has a profile");
        }

        if (major == null || major.isBlank()) throw new IllegalArgumentException("Major is required");
        if (year == null) throw new IllegalArgumentException("Year is required");
        if (year < 0) throw new IllegalArgumentException("Year cannot be negative");

        Profile profile = new Profile();
        profile.setPlayer(player);
        profile.setDescription(description);
        profile.setMajor(major.trim());
        profile.setYear(year);

        return PublicProfileDto.from(profileRepository.save(profile));
    }

    @Transactional
    public PublicProfileDto updateProfile(Player player, String description, String major, Integer year) {
        Profile profile = profileRepository.findByPlayerAndIsDeletedFalse(player)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));

        if (major != null && !major.isBlank()) profile.setMajor(major.trim());
        if (description != null) profile.setDescription(description);
        if (year != null) {
            if (year < 0) throw new IllegalArgumentException("Year cannot be negative");
            profile.setYear(year);
        }

        return PublicProfileDto.from(profileRepository.save(profile));
    }

    @Transactional
    public void deleteProfile(Player player) {
        Profile profile = profileRepository.findByPlayerAndIsDeletedFalse(player)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));

        profile.softDelete();
        profileRepository.save(profile);
    }


    @Transactional(readOnly = true)
    public PublicProfileDto getProfile(
        Player player
    ) {
        return profileRepository.findByPlayerAndIsDeletedFalse(player)
                .map(PublicProfileDto::from)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));
    }
}