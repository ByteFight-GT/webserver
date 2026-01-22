package com.example.botfightwebserver.auth.domain.dto;

import com.example.botfightwebserver.player.domain.PlayerUsername;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterUserDto {
    @NotBlank(message = "Email is required.")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    @PlayerUsername
    private String name;
}