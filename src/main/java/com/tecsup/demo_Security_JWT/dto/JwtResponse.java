package com.tecsup.demo_Security_JWT.dto;

import java.util.List;

public record JwtResponse(String accessToken, String tokenType, Long id,
                          String username, String email, List<String> roles) { }
