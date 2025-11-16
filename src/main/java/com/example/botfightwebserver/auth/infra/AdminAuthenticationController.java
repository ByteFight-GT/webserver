package com.example.botfightwebserver.auth.infra;

import com.example.botfightwebserver.auth.application.SupabaseService;
import com.example.botfightwebserver.auth.application.UserService;
import com.example.botfightwebserver.auth.domain.ImpersonateUserDto;
import com.example.botfightwebserver.auth.domain.RegisterUserDto;
import com.example.botfightwebserver.auth.domain.SupabaseDtos;
import com.example.botfightwebserver.auth.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1/admin/auth")
@RestController
@RequiredArgsConstructor
public class AdminAuthenticationController {
    private final SupabaseService supabaseService;
    private final UserService userService;

    @PostMapping("/impersonate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SupabaseDtos.SupabaseMagicLink> adminImpersonateUser(@Valid @RequestBody ImpersonateUserDto dto) {
        User user = userService.findByUuid(dto.getUuid()).orElseThrow();
        var data = supabaseService.createMagicSignInLink(user.getEmail());

        return ResponseEntity.ok(data);
    }
}
