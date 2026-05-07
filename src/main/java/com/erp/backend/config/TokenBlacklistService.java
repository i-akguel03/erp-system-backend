package com.erp.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-Memory Token-Blacklist für invalidierte JWTs (Logout, Passwortänderung).
 * Einträge werden automatisch nach Ablauf des Tokens bereinigt.
 *
 * Hinweis: Bei mehreren Instanzen (Horizontal Scaling) muss diese Blacklist
 * durch Redis o.ä. ersetzt werden.
 */
@Service
public class TokenBlacklistService {

    private static final Logger logger = LoggerFactory.getLogger(TokenBlacklistService.class);

    // JTI → Ablaufzeit des Tokens
    private final ConcurrentHashMap<String, Instant> blacklistedTokens = new ConcurrentHashMap<>();

    public void blacklist(String jti, Instant expiration) {
        blacklistedTokens.put(jti, expiration);
        logger.debug("Token blacklisted: jti={}", jti);
    }

    public boolean isBlacklisted(String jti) {
        return blacklistedTokens.containsKey(jti);
    }

    @Scheduled(fixedRate = 3_600_000) // stündlich
    public void cleanupExpiredTokens() {
        Instant now = Instant.now();
        int before = blacklistedTokens.size();
        blacklistedTokens.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
        int removed = before - blacklistedTokens.size();
        if (removed > 0) {
            logger.debug("Token blacklist cleanup: {} abgelaufene Einträge entfernt", removed);
        }
    }

    public int size() {
        return blacklistedTokens.size();
    }
}
