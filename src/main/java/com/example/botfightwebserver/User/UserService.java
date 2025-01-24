package com.example.botfightwebserver.User;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {
    public boolean hasAccess(String requestedUserId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ADMIN"))) {
            return true;
        }
        String currentUserId = (String) auth.getPrincipal();
        return currentUserId.equals(requestedUserId);
    }

    public boolean hasAccess(UUID requestedUserId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ADMIN"))) {
            return true;
        }
        return auth.getPrincipal().equals(requestedUserId);
    }

    public boolean hasAccess() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ADMIN"))) {
            return true;
        }
        return false;
    }
}