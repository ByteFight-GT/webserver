package org.bytefight.webserver.auth.application;

import org.bytefight.webserver.auth.domain.EmailAlreadyRegisteredException;
import org.bytefight.webserver.auth.domain.RegistrationException;
import org.bytefight.webserver.auth.domain.User;
import org.bytefight.webserver.auth.domain.UsernameAlreadyExistsException;
import org.bytefight.webserver.auth.domain.dto.RegisterUserDto;
import org.bytefight.webserver.auth.domain.dto.SupabaseDtos;
import org.bytefight.webserver.auth.infra.UserRepository;
import org.bytefight.webserver.player.infra.PlayerRepository;
import org.bytefight.webserver.player.application.PlayerService;
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
    private final AuthService authService;

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
        return signup(input, false);
    }

    @Transactional
    public User signup(RegisterUserDto input, boolean confirmEmail) {
        String normalizedEmail = normalize(input.getEmail());
        String normalizedUsername = normalize(input.getName());

        if(userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException(input.getEmail());
        }

        if(playerRepository.existsByUsernameNormalized(normalizedUsername)) {
            throw new UsernameAlreadyExistsException(input.getName());
        }

        try {
            User user = new User();
            user.setUuid(UUID.randomUUID()); // Set this to a random UUID first, then overwrite with UUID from Supabase
            user.setEmail(normalizedEmail);
            user = userRepository.save(user);

            playerService.createPlayer(user, input.getName());

            SupabaseDtos.SupabaseUser supabaseUser = authService.createUser(normalizedEmail, input.getPassword(), confirmEmail, Map.of(), Map.of());

            user.setUuid(UUID.fromString(supabaseUser.id()));
            user = userRepository.save(user);

            return user;
        } catch(AuthService.AuthServiceException e) {
            throw new RegistrationException(e.getMessage());
        }
    }
}
