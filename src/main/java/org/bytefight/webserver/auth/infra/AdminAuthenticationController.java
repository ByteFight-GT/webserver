package org.bytefight.webserver.auth.infra;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.bytefight.webserver.auth.application.AuthService;
import org.bytefight.webserver.auth.application.UserService;
import org.bytefight.webserver.auth.domain.User;
import org.bytefight.webserver.auth.domain.dto.ImpersonateUserDto;
import org.bytefight.webserver.auth.domain.dto.SupabaseDtos;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1/admin/auth")
@RestController
@RequiredArgsConstructor
public class AdminAuthenticationController {
  private final AuthService authService;
  private final UserService userService;

  @PostMapping("/impersonate")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<SupabaseDtos.SupabaseMagicLink> adminImpersonateUser(
      @Valid @RequestBody ImpersonateUserDto dto) {
    User user = userService.findByUuid(dto.getUuid()).orElseThrow();
    var data = authService.createMagicSignInLink(user.getEmail());

    return ResponseEntity.ok(data);
  }
}
