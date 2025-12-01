package org.tms.controller;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.tms.dto.AuthenticationRequest;
import org.tms.dto.AuthenticationResponse;
import org.tms.entity.Role;
import org.tms.entity.UserEntity;
import org.tms.repository.UserRepository;
import org.tms.service.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tms.audit.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private JwtUtil jwtUtil;
    @Autowired
    private AuditService auditService;

    @PostMapping("/register")
    public String register(@RequestBody UserEntity userEntity, HttpServletRequest httpRequest) {
        log.info("Registering new user: {}", userEntity.getUsername());
        userEntity.setPassword(passwordEncoder.encode(userEntity.getPassword()));
        if (userEntity.getRole() == null) userEntity.setRole(Role.USER);
        userRepository.save(userEntity);
        auditService.audit("USER_REGISTERED", userEntity.getUsername(), "User registered successfully");
        auditService.auditDb(
                "USER_REGISTERED",
                userEntity.getUsername(),
                userEntity,
                "User registered successfully",
                httpRequest.getRequestURI(),
                httpRequest.getMethod(),
                MDC.get("requestId")
        );
        log.info("User registered: {}", userEntity.getUsername());
        return "User registered successfully";
    }

    @PostMapping("/login")
    public AuthenticationResponse login(@RequestBody AuthenticationRequest request, HttpServletRequest httpRequest) {
        log.info("Login attempt for user: {}", request.getUsername());
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        // if authentication fails, Spring will throw exception automatically
        String token = jwtUtil.generateToken(request.getUsername());
        auditService.audit("USER_LOGGED_IN", request.getUsername(), "JWT issued");
        log.info("Login successful for user: {}", request.getUsername());
        return new AuthenticationResponse(token);
    }
}

