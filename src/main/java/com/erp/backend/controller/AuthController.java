package com.erp.backend.controller;

import com.erp.backend.config.JwtUtil;
import com.erp.backend.dto.AuthRequest;
import com.erp.backend.dto.AuthResponse;
import com.erp.backend.dto.RefreshRequest;
import com.erp.backend.dto.RegisterRequest;
import com.erp.backend.service.UserDetailsServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authenticationManager,
                          UserDetailsService userDetailsService,
                          JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService; // wird automatisch UserDetailsServiceImpl sein
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails user = (UserDetails) auth.getPrincipal();
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (((UserDetailsServiceImpl) userDetailsService)
                .userExists(request.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User already exists");
        }

        ((UserDetailsServiceImpl) userDetailsService).createUser(request.getUsername(), request.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered");
    }


    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        try {
            if (jwtUtil.isTokenValid(refreshToken)) {
                String username = jwtUtil.extractUsername(refreshToken);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                String newAccessToken = jwtUtil.generateAccessToken(userDetails);
                String newRefreshToken = jwtUtil.generateRefreshToken(userDetails);

                Map<String, Object> response = new HashMap<>();
                response.put("accessToken", newAccessToken);
                response.put("refreshToken", newRefreshToken);
                response.put("expiresIn", jwtUtil.getAccessTokenValidity());

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid refresh token"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Token refresh failed"));
        }
    }
}
