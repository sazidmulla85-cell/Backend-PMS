package com.example.pms.backendpms.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final Key signingKey;
  private final long expirationMs;

  public JwtService(
      @Value("${pms.security.jwt.secret}") String secret,
      @Value("${pms.security.jwt.expiration-ms:86400000}") long expirationMs
  ) {
    this.signingKey = createSigningKey(secret);
    this.expirationMs = expirationMs;
  }

  public String generateToken(AppUserPrincipal principal) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("role", principal.getRole().name());
    claims.put("propertyId", principal.getPropertyId());

    Instant issuedAt = Instant.now();
    Instant expiresAt = issuedAt.plusMillis(expirationMs);

    return Jwts.builder()
        .claims(claims)
        .subject(String.valueOf(principal.getUserId()))
        .issuedAt(Date.from(issuedAt))
        .expiration(Date.from(expiresAt))
        .signWith(signingKey)
        .compact();
  }

  public Long extractUserId(String token) {
    return Long.valueOf(extractAllClaims(token).getSubject());
  }

  public Instant extractExpiration(String token) {
    return extractAllClaims(token).getExpiration().toInstant();
  }

  public boolean isTokenValid(String token, AppUserPrincipal principal) {
    Claims claims = extractAllClaims(token);
    return claims.getSubject().equals(String.valueOf(principal.getUserId()))
        && claims.getExpiration().after(new Date())
        && principal.isEnabled();
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser()
        .verifyWith((javax.crypto.SecretKey) signingKey)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  private Key createSigningKey(String secret) {
    byte[] secretBytes;
    try {
      secretBytes = Decoders.BASE64.decode(secret);
    } catch (RuntimeException exception) {
      secretBytes = secret.getBytes(StandardCharsets.UTF_8);
    }

    if (secretBytes.length < 32) {
      byte[] padded = new byte[32];
      System.arraycopy(secretBytes, 0, padded, 0, Math.min(secretBytes.length, 32));
      for (int index = secretBytes.length; index < padded.length; index++) {
        padded[index] = (byte) ('a' + (index % 26));
      }
      secretBytes = padded;
    }

    return Keys.hmacShaKeyFor(secretBytes);
  }
}
