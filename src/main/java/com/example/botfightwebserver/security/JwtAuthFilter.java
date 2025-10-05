package com.example.botfightwebserver.security;

import com.example.botfightwebserver.auth.JwtService;
import com.example.botfightwebserver.auth.User;
import com.example.botfightwebserver.auth.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerExceptionResolver;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final HandlerExceptionResolver handlerExceptionResolver;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;

    @Value("${ADMINS}")
    private String admins;

    private Set<String> adminIds;

    @PostConstruct  // Load admins when bean is created
    public void loadAdmins() {
        adminIds=Arrays.stream(StringUtils.trimAllWhitespace(admins).split(",")).collect(Collectors.toSet());
    }

    public JwtAuthFilter(UserRepository userRepository, UserDetailsService userDetailsService, JwtService jwtService, HandlerExceptionResolver handlerExceptionResolver) {
        this.userRepository = userRepository;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);
            final String userEmail = jwtService.extractUsername(jwt);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (userEmail != null && authentication == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                Optional<User> user = userRepository.findByEmail(userEmail);
                List<String> roles = new ArrayList<>();
                roles.add("USER");
                if (user.isPresent() && adminIds.contains(user.get().getUuid().toString())) {
                    roles.add("ADMIN");
                }

                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role)) // Spring Security expects "ROLE_" prefix
                        .toList();

                if(jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            authorities
                    );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

            filterChain.doFilter(request, response);
        } catch(Exception exception) {
            handlerExceptionResolver.resolveException(request, response, null, exception);
        }
    }
}
