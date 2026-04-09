package org.bytefight.webserver.auth.infra;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.bytefight.webserver.auth.domain.dto.SelfAuthUserDto;
import org.bytefight.webserver.user.application.UserService;
import org.bytefight.webserver.user.domain.User;
import org.bytefight.webserver.user.domain.dto.RegisterUserDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "General endpoints for authentication")
@RequestMapping("/api/v1/auth")
@RestController
@RequiredArgsConstructor
public class AuthenticationController {
  private final UserService userService;

  @Operation(
      operationId = "getCurrentUser",
      summary = "Get current authenticated user",
      description =
          """
          Returns a concise summary of the currently authenticated user (using JWT bearer token in header).
          Can be used to quickly check if the JWT token is valid or view a summary of user details.
          """)
  @GetMapping("/me")
  public ResponseEntity<SelfAuthUserDto> getCurrentUser(@AuthenticationPrincipal User user) {
    return ResponseEntity.ok(SelfAuthUserDto.from(user));
  }

  @PostMapping("/signup")
  public ResponseEntity<Void> register(@Valid @RequestBody RegisterUserDto registerUserDto) {
    userService.signup(registerUserDto);

    return ResponseEntity.ok().build();
  }
}
