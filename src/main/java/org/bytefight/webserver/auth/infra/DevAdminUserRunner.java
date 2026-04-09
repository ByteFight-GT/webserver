package org.bytefight.webserver.auth.infra;

import org.bytefight.webserver.user.application.UserService;
import org.bytefight.webserver.user.domain.User;
import org.bytefight.webserver.user.domain.dto.RegisterUserDto;
import org.bytefight.webserver.user.infra.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.dev.admin-user.enabled", havingValue = "true")
public class DevAdminUserRunner implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(DevAdminUserRunner.class);
  private final DevAdminUserProperties properties;
  private final UserRepository userRepository;
  private final UserService userService;

  public DevAdminUserRunner(
      DevAdminUserProperties properties, UserRepository userRepository, UserService userService) {
    this.properties = properties;
    this.userRepository = userRepository;
    this.userService = userService;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (isBlank(properties.getEmail())
        || isBlank(properties.getPassword())
        || isBlank(properties.getUsername())) {
      log.warn("Default user seed skipped: missing email/password/username");
      return;
    }

    if (userRepository.existsByEmailIgnoreCase(properties.getEmail())) {
      log.info("Default user seed skipped: user already exists");
      return;
    }

    RegisterUserDto dto = new RegisterUserDto();
    dto.setEmail(properties.getEmail());
    dto.setPassword(properties.getPassword());
    dto.setName(properties.getUsername());

    User user = userService.signup(dto, true);
    user.setAdmin(true);
    userRepository.save(user);

    log.info("Default user seeded: {}", properties.getEmail());
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
