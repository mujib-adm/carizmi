package io.carizmi.domain.identity.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expirationMinutes}")
    private int expMin;

    @Value("${jwt.issuer:carizmi}")
    private String issuer;

    public String generateAccessToken(UserDetails userDetails) {
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(role -> role.replace("ROLE_", ""))
                .collect(Collectors.toList());

        return Jwts.builder()
                .id(java.util.UUID.randomUUID().toString())
                .subject(userDetails.getUsername())
                .claim("roles", roles)
                .issuer(issuer)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(expMin, ChronoUnit.MINUTES)))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret)), Jwts.SIG.HS512)
                .compact();
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getRemainingExpirationSeconds(String token) {
        try {
            Claims claims = getClaims(token);
            Instant expiration = claims.getExpiration().toInstant();
            return Math.max(0, Duration.between(Instant.now(), expiration).getSeconds());
        } catch (Exception e) {
            return 0;
        }
    }
}