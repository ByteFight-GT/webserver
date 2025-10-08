package com.example.botfightwebserver.auth.application;

import com.example.botfightwebserver.auth.domain.*;
import com.example.botfightwebserver.auth.infra.UserRepository;
import com.example.botfightwebserver.player.PlayerRepository;
import com.example.botfightwebserver.player.PlayerService;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final PlayerService playerService;

    static String normalize(String raw) {
        return raw == null ? null : raw.trim().toLowerCase();
    }

    public User signup(RegisterUserDto input) {
        String normalizedEmail = normalize(input.getEmail());
        String normalizedUsername = normalize(input.getName());

        if(userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException(input.getEmail());
        }

        if(playerRepository.existsByNameIgnoreCase(normalizedUsername)) {
            throw new UsernameAlreadyExistsException(input.getName());
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(input.getPassword()));

        user = userRepository.save(user);

        playerService.createPlayer(user, input.getName(), null);

        return user;
    }

    public User authenticate(LoginUserDto input) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        input.email(),
                        input.password()
                )
        );

        return userRepository.findByEmail(input.email())
                .orElseThrow();
    }
}
