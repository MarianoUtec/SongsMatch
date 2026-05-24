package com.musicmatch.auth.dto.request;

import jakarta.validation.constraints.*;

// RegisterRequest.java
public record RegisterRequest(
    @NotBlank @Size(min = 2, max = 50) String name,
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8, max = 100)
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
             message = "Password must have uppercase, lowercase and number")
    String password
) {}
