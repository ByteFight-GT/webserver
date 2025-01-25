//package com.example.botfightwebserver.security;
//
//import io.jsonwebtoken.Jwts;
//import io.jsonwebtoken.security.Keys;
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.test.util.ReflectionTestUtils;
//
//import java.util.Base64;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//@ExtendWith(MockitoExtension.class)
//class JwtAuthFilterTest {
//    @Mock
//    private HttpServletRequest request;
//    @Mock
//    private HttpServletResponse response;
//    @Mock
//    private FilterChain filterChain;
//
//    @InjectMocks
//    private JwtAuthFilter jwtAuthFilter;
//
//    @BeforeEach
//    void setUp() {
//        ReflectionTestUtils.setField(jwtAuthFilter, "jwtSecret", "5EBZXvyjCEspndvK/18edD7qHwXuy7H+HLOiYeDEQz4=");
//        SecurityContextHolder.clearContext(); // Clear security context before each test
//    }
//
//    @Test
//    void shouldAuthenticateValidToken() throws Exception {
//        String token = generateValidToken();
//        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
//
//        jwtAuthFilter.doFilterInternal(request, response, filterChain);
//
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        assertNotNull(auth);
//        assertEquals("test-user-id", auth.getPrincipal());
//        verify(filterChain).doFilter(request, response);
//    }
//
//    @Test
//    void shouldRejectInvalidToken() throws Exception {
//        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
//
//        jwtAuthFilter.doFilterInternal(request, response, filterChain);
//
//        verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), anyString());
//        assertNull(SecurityContextHolder.getContext().getAuthentication());
//    }
//
//    private String generateValidToken() {
//        return Jwts.builder()
//            .setSubject("test-user-id")
//            .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode("5EBZXvyjCEspndvK/18edD7qHwXuy7H+HLOiYeDEQz4=")))
//            .compact();
//    }
//}