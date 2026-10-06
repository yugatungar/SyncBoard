package com.syncboard.backend.controller;

import com.syncboard.backend.dto.AuthResponse;
import com.syncboard.backend.dto.LoginRequest;
import com.syncboard.backend.dto.RegisterRequest;
import com.syncboard.backend.service.AuthService;
import com.syncboard.backend.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RateLimitService rateLimitService;
    private final int authLimit;

    public AuthController(AuthService authService,
                          RateLimitService rateLimitService,
                          @Value("${syncboard.ratelimit.auth.limit:100}") int authLimit) {
        this.authService = authService;
        this.rateLimitService = rateLimitService;
        this.authLimit = authLimit;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest servletRequest) {
        String clientIp = getClientIp(servletRequest);
        rateLimitService.checkRateLimit("ratelimit:auth:register:" + clientIp, authLimit, 60);

        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        String clientIp = getClientIp(servletRequest);
        rateLimitService.checkRateLimit("ratelimit:auth:login:" + clientIp, authLimit, 60);

        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
