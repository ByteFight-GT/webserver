package com.example.botfightwebserver.auth.application;

import com.example.botfightwebserver.auth.domain.User;
import com.example.botfightwebserver.auth.infra.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtToUserAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final UserRepository userRepository;
    private final UserService userService;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UUID sub = UUID.fromString(jwt.getSubject());

        User user = userRepository.findByUuid(sub).orElseThrow();

        List<GrantedAuthority> auths = new ArrayList<>();
        auths.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (user.isAdmin()) auths.add(new SimpleGrantedAuthority("ROLE_ADMIN"));

        var auth = new UsernamePasswordAuthenticationToken(user, jwt, auths);
        return auth;
    }
}
