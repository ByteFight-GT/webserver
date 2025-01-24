package com.example.botfightwebserver.matchMaking;

import com.example.botfightwebserver.User.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchMakerController {

    private final MatchMaker matchMaker;
    private final UserService userService;

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Void> generateMatches() {
        if (!userService.hasAccess()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        matchMaker.generateMatches();
        return ResponseEntity.ok().build();
    }
}