package com.example.botfightwebserver.gameMatch;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rescheduler")
public class GameMatchReschedulerController {

    private final GameMatchService gameMatchService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public void rescheduleMatches() {
        gameMatchService.rescheduleFailedAndStaleMatches();
    }

}
