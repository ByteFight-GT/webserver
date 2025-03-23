package com.example.botfightwebserver.tournament;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TournamentSetService {

    private final TournamentSetRepository repository;

    public TournamentSet save(TournamentSet tournamentSet) {
        repository.save(tournamentSet);
        return tournamentSet;
    }

    public TournamentSet findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("id " + id + " doesn't exist"));
    }
}
