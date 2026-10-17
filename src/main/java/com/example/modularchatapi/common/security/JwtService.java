package com.example.modularchatapi.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

// .NET'teki JwtSecurityTokenHandler karşılığı: token üretir ve doğrular
@Service
public class JwtService {

    private final SecretKey key;
    private final long expiration;

    public JwtService(@Value("${application.security.jwt.secret-key}") String secretKey,
                      @Value("${application.security.jwt.expiration}") long expiration) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
        this.expiration = expiration;
    }

    public String generateToken(UserDetails user, Map<String, Object> extraClaims) {
        Date now = new Date();
        return Jwts.builder()
                .claims(extraClaims)
                .subject(user.getUsername())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(key)
                .compact();
    }

    public String extractSubject(String token) {
        return parse(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails user) {
        Claims claims = parse(token);
        return claims.getSubject().equals(user.getUsername()) && claims.getExpiration().after(new Date());
    }

    private Claims parse(String token) {
        // İmza geçersizse veya süresi dolmuşsa JwtException fırlatır
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
