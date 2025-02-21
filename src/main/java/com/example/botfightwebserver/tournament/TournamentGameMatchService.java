package com.example.botfightwebserver.tournament;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TournamentGameMatchService{

    private final TournamentGameMatchRepository gameMatchRepository;

    public TournamentGameMatch save(TournamentGameMatch tournamentGameMatch) {
        return gameMatchRepository.save(tournamentGameMatch);
    }

    public TournamentGameMatch findById(Long id) {
        return gameMatchRepository.findById(id).orElse(null);
    }
}
