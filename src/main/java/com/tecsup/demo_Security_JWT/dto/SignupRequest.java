package com.tecsup.demo_Security_JWT.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record SignupRequest(@NotBlank @Size(max = 80) String username,
                            @NotBlank @Email @Size(max = 160) String email,
                            @NotBlank @Size(min = 8, max = 100) String password,
                            // Accepted for compatibility with the lab payload; never trusted.
                            Set<String> roles) { }
