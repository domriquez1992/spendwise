package com.domriquez.spendwise.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Issues and verifies signed JSON Web Tokens (HMAC-SHA). The signing key is derived from a
 * configured secret; signature and expiry verification are delegated to jjwt, which throws a
 * {@link io.jsonwebtoken.JwtException} for any tampered, malformed, or expired token.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        // HMAC-SHA-256 requires a key of at least 256 bits; the configured secret must be
        // long enough (>= 32 bytes) or jjwt will reject it as a weak key.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String username) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    /**
     * Returns the username held in the token's subject claim, verifying the signature and
     * expiry along the way. Throws {@link io.jsonwebtoken.JwtException} if the token is invalid.
     */
    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}
