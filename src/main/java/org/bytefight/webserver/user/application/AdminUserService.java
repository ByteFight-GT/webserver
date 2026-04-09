package org.bytefight.webserver.user.application;

import java.util.List;

import org.bytefight.webserver.user.domain.dto.AdminUserWithPlayerDto;
import org.bytefight.webserver.user.infra.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
