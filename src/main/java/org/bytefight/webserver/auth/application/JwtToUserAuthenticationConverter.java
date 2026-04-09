package org.bytefight.webserver.auth.application;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bytefight.webserver.user.application.UserService;
import org.bytefight.webserver.user.domain.User;
import org.bytefight.webserver.user.infra.UserRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtToUserAuthenticationConverter
    implements Converter<Jwt, AbstractAuthenticationToken> {
  private final UserRepository userRepository;
  private final UserService userService;

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    UUID sub = UUID.fromString(jwt.getSubject());

    Optional<User> userOptional = userRepository.findByUuid(sub);

    if (userOptional.isEmpty()) {
      return new JwtAuthenticationToken(jwt, List.of());
    }

    User user = userOptional.get();

    List<GrantedAuthority> auths = new ArrayList<>();
    auths.add(new SimpleGrantedAuthority("ROLE_USER"));
    if (user.isAdmin()) auths.add(new SimpleGrantedAuthority("ROLE_ADMIN"));

    var auth = new UsernamePasswordAuthenticationToken(user, jwt, auths);
    return auth;
  }
}
