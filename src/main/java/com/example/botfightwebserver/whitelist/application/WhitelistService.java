package com.example.botfightwebserver.whitelist.application;

import com.example.botfightwebserver.auth.domain.User;
import com.example.botfightwebserver.auth.infra.UserRepository;
import com.example.botfightwebserver.competition.domain.Competition;
import com.example.botfightwebserver.whitelist.infra.WhitelistEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WhitelistService {
    private final WhitelistEntryRepository whitelistEntryRepository;

    public boolean isCompetitionParticipationAllowed(Competition competition, User user) {
        if(!competition.isWhitelisted()) return true;

        return whitelistEntryRepository.existsByCompetitionAndEmail(competition, user.getEmail());
    }
}
