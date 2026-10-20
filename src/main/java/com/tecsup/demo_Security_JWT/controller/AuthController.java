package com.tecsup.demo_Security_JWT.controller;

import com.tecsup.demo_Security_JWT.dto.JwtResponse;
import com.tecsup.demo_Security_JWT.dto.LoginRequest;
import com.tecsup.demo_Security_JWT.dto.MessageResponse;
import com.tecsup.demo_Security_JWT.dto.SignupRequest;
import com.tecsup.demo_Security_JWT.entity.Role;
import com.tecsup.demo_Security_JWT.entity.User;
import com.tecsup.demo_Security_JWT.jwt.JwtUtils;
import com.tecsup.demo_Security_JWT.repository.RoleRepository;
import com.tecsup.demo_Security_JWT.repository.UserRepository;
import com.tecsup.demo_Security_JWT.service.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    public AuthController(UserRepository users, RoleRepository roles,
                          PasswordEncoder passwordEncoder,
                          AuthenticationManager authenticationManager, JwtUtils jwtUtils) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/signup")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody SignupRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase();
        if (users.existsByUsername(username) || users.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new MessageResponse("El nombre de usuario o correo ya existe"));
        }
        Role userRole = roles.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("No existe el rol USER"));
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRoles(Set.of(userRole));
        users.save(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MessageResponse("Usuario registrado correctamente"));
    }

    @PostMapping("/signin")
    public JwtResponse authenticate(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email().trim().toLowerCase(), request.password()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl user = (UserDetailsImpl) authentication.getPrincipal();
        String token = jwtUtils.generateJwtToken(authentication);
        List<String> authorities = user.getAuthorities().stream()
                .map(authority -> authority.getAuthority()).toList();
        return new JwtResponse(token, "Bearer", user.id(), user.username(), user.email(), authorities);
    }
}
