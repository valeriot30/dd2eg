package com.dd2eg.backend.service;

import com.dd2eg.backend.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Service
public class TokenService {

    private final String SECRET = "o4V8AQdmad9MLJowczIxrtgAFC4SigFqLSgxJMxY1GM";

    public String generateToken(User user) {
        return Jwts.builder()
                .subject(user.getId())
                .claim("role", user.getUserType().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()))
                .compact();
    }

    public String extractUserId(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes()))
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean isTokenValid(String jwt, UserDetails userDetails) {
        final String userId = extractUserId(jwt);

        return (userId.equals(((User) userDetails).getId())) && !isTokenExpired(jwt);
    }

    private boolean isTokenExpired(String jwt) {
        return extractExpiration(jwt).before(new java.util.Date());
    }

    private java.util.Date extractExpiration(String jwt) {
        return extractClaim(jwt, io.jsonwebtoken.Claims::getExpiration);
    }

    public <T> T extractClaim(String jwt, java.util.function.Function<io.jsonwebtoken.Claims, T> claimsResolver) {
        final io.jsonwebtoken.Claims claims = extractAllClaims(jwt);
        return claimsResolver.apply(claims);
    }

    public String extractUsername(String jwt) {
        return extractClaim(jwt, io.jsonwebtoken.Claims::getSubject);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Key getSignInKey() {
        byte[] keyBytes = SECRET.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
