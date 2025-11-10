package com.example.botfightwebserver.auth.infra;

import com.example.botfightwebserver.auth.application.SupabaseService;
import com.example.botfightwebserver.auth.application.UserService;
import com.example.botfightwebserver.auth.domain.RegisterUserDto;
import com.example.botfightwebserver.auth.domain.SelfUserDto;
import com.example.botfightwebserver.auth.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Auth")
@RequestMapping("/api/v1/auth")
@RestController
@RequiredArgsConstructor
public class AuthenticationController {
    private final UserService userService;

    @Operation(
            operationId = "getCurrentUser",
            summary = "Get current authenticated user"
    )
    @GetMapping("/me")
    public ResponseEntity<SelfUserDto> getCurrentUser(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(SelfUserDto.from(user));
    }

    @PostMapping("/signup")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterUserDto registerUserDto) {
        userService.signup(registerUserDto);

        return ResponseEntity.ok().build();
    }
}