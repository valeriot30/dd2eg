package com.dd2eg.backend.auth;

import com.dd2eg.backend.users.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

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

}
