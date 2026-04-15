package org.bytefight.webserver.user.application;

import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.bytefight.webserver.storage.application.LocalStorageService;
import org.bytefight.webserver.storage.domain.DownloadLinkDto;
import org.bytefight.webserver.storage.domain.FileRecord;
import org.bytefight.webserver.user.domain.User;
import org.bytefight.webserver.user.domain.dto.AdminUserWithPlayerAndResumeDto;
import org.bytefight.webserver.user.domain.dto.ResumeDto;
import org.bytefight.webserver.user.infra.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class AdminUserService {
  private final UserRepository userRepository;
  private final LocalStorageService localStorageService;

  public AdminUserService(UserRepository userRepository, LocalStorageService localStorageService) {
    this.userRepository = userRepository;
    this.localStorageService = localStorageService;
  }

  public Page<AdminUserWithPlayerAndResumeDto> listUsers(Pageable pageable, List<Long> userIds) {
    if (userIds != null && !userIds.isEmpty()) {
      return userRepository.findAllWithPlayersByIdIn(userIds, pageable);
    }
    return userRepository.findAllWithPlayers(pageable);
  }

  public ResumeDto getResume(UUID userUuid) {
    User user = userRepository.findByUuid(userUuid).orElseThrow();
    FileRecord resume = user.getResume();

    if (resume == null) {
      throw new NoSuchElementException("No resume found");
    }

    DownloadLinkDto link =
        localStorageService.getDownloadLink(resume.getUuid().toString(), Duration.ofMinutes(5));
    return ResumeDto.from(link, user);
  }
}
