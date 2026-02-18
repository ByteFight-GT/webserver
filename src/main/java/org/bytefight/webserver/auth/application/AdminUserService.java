package org.bytefight.webserver.auth.application;

import org.bytefight.webserver.auth.domain.dto.AdminUserWithPlayerDto;
import org.bytefight.webserver.auth.infra.UserRepository;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Service
public class AdminUserService {
    private final UserRepository userRepository;

    public AdminUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Page<AdminUserWithPlayerDto> listUsers(Pageable pageable, List<Long> userIds) {
        if (userIds != null && !userIds.isEmpty()) {
            return userRepository.findAllWithPlayersByIdIn(userIds, pageable);
        }
        return userRepository.findAllWithPlayers(pageable);
    }
}
