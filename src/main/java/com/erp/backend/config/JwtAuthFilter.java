package com.erp.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtAuthFilter(UserDetailsService userDetailsService,
                         JwtUtil jwtUtil,
                         TokenBlacklistService tokenBlacklistService) {
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);

        try {
            // 1. Token-Typ: nur Access-Tokens für API-Requests akzeptieren
            if (!jwtUtil.isAccessToken(token)) {
                logger.warn("Abgelehnter Request: Refresh-Token als Access-Token verwendet");
                filterChain.doFilter(request, response);
                return;
            }

            // 2. JTI extrahieren und gegen Blacklist prüfen
            String jti = jwtUtil.extractJti(token);
            if (tokenBlacklistService.isBlacklisted(jti)) {
                logger.warn("Abgelehnter Request: Token wurde invalidiert (jti={})", jti);
                filterChain.doFilter(request, response);
                return;
            }

            // 3. Username extrahieren und User authentifizieren
            String username = jwtUtil.extractUsername(token);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails user = userDetailsService.loadUserByUsername(username);

                if (jwtUtil.isTokenValid(token, user)) {
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }

        } catch (Exception e) {
            // Ungültiges/manipuliertes Token → Request geht unauthentifiziert weiter
            // Spring Security gibt dann 401 zurück
            logger.debug("JWT-Verarbeitung fehlgeschlagen: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
