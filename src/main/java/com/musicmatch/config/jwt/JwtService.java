package com.musicmatch.config.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@Slf4j
public class JwtService {

    @Value("${spring.security.jwt.secret}")
    private String secret;

    @Value("${spring.security.jwt.expiration}")
    private long expiration;

    @Value("${spring.security.jwt.refresh-expiration}")
    private long refreshExpiration;

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, expiration);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails, refreshExpiration);
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiry) {
        return Jwts.builder()
            .claims(extraClaims)
            .subject(userDetails.getUsername())
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + expiry))
            .signWith(getSignKey())
            .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSignKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private SecretKey getSignKey() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret is not configured");
        }

        byte[] keyBytes = null;

        // Try Base64 decoding first (common case)
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (Exception ignored) {
        }

        // If not base64, try hex decoding (common in test fixtures)
        if (keyBytes == null) {
            try {
                if (secret.matches("^[0-9a-fA-F]+$") && (secret.length() % 2 == 0)) {
                    int len = secret.length();
                    byte[] bytes = new byte[len / 2];
                    for (int i = 0; i < len; i += 2) {
                        bytes[i / 2] = (byte) ((Character.digit(secret.charAt(i), 16) << 4)
                            + Character.digit(secret.charAt(i + 1), 16));
                    }
                    keyBytes = bytes;
                }
            } catch (Exception ignored) {
            }
        }

        // Fallback: derive a 256-bit key from the configured secret using SHA-256
        if (keyBytes == null) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                keyBytes = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("Unable to derive JWT signing key", e);
            }
        }

        // Ensure key is at least 256 bits (32 bytes) by hashing if necessary
        if (keyBytes.length < 32) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                keyBytes = digest.digest(keyBytes);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("Unable to expand JWT signing key", e);
            }
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }
}
