package com.example.botfightwebserver.auth.infra;

import com.example.botfightwebserver.auth.application.UserService;
import com.example.botfightwebserver.auth.domain.RegisterUserDto;
import com.example.botfightwebserver.auth.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/v1/auth")
@RestController
@RequiredArgsConstructor
public class AuthenticationController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<User> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(user);
    }

    @PostMapping("/signup")
    public ResponseEntity<User> register(@Valid @RequestBody RegisterUserDto registerUserDto) {
        userService.signup(registerUserDto);

        return ResponseEntity.ok(null);
    }
}