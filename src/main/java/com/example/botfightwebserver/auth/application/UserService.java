package com.example.botfightwebserver.auth.application;

import com.example.botfightwebserver.auth.domain.*;
import com.example.botfightwebserver.auth.infra.UserRepository;
import com.example.botfightwebserver.player.infra.PlayerRepository;
import com.example.botfightwebserver.player.application.PlayerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PlayerRepository playerRepository;
    private final PlayerService playerService;
    private final SupabaseService supabaseService;

    static String normalize(String raw) {
        return raw == null ? null : raw.trim().toLowerCase();
    }

    @Transactional
    public User createFromJwt(Jwt jwt) {
        UUID uuid = UUID.fromString(jwt.getSubject());
        String email = (String) jwt.getClaims().get("email");

        User u = new User();
        u.setUuid(uuid);
        u.setEmail(email.toLowerCase());
        return userRepository.save(u);
    }

    public Optional<User> findByUuid(String uuid) {
        return userRepository.findByUuid(UUID.fromString(uuid));
    }

    @Transactional
    public User signup(RegisterUserDto input) {
        String normalizedEmail = normalize(input.getEmail());
        String normalizedUsername = normalize(input.getName());

        if(userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException(input.getEmail());
        }

        if(playerRepository.existsByNameIgnoreCase(normalizedUsername)) {
            throw new UsernameAlreadyExistsException(input.getName());
        }

        try {
            SupabaseDtos.SupabaseUser supabaseUser = supabaseService.createUser(normalizedEmail, input.getPassword(), false, Map.of(), Map.of());

            User user = new User();
            user.setUuid(UUID.fromString(supabaseUser.id()));
            user.setEmail(normalizedEmail);
            user = userRepository.save(user);

            playerService.createPlayer(user, input.getName(), null);

            return user;
        } catch(SupabaseService.SupabaseServiceException e) {
            throw new RegistrationException(e.getMessage());
        }
    }
}
