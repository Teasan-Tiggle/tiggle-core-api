package com.example.tiggle.security.jwt;

import com.example.tiggle.exception.auth.AuthException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-expiration-time}")
    private long accessTokenExpirationTime;

    @Value("${jwt.refresh-expiration-time}")
    private long refreshTokenExpirationTime;

    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public String generateAccessToken(Long userId, String encryptedUserKey) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationTime);

        return Jwts.builder()
                .claim("userId", userId)
                .claim("userKey", encryptedUserKey)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpirationTime);

        return Jwts.builder()
                .claim("userId", userId)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser().verifyWith((javax.crypto.SecretKey) key).build().parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException ex) {
            throw new AuthException("JWT_EXPIRED", "토큰이 만료되었습니다.");
        } catch (UnsupportedJwtException ex) {
            throw new AuthException("JWT_UNSUPPORTED", "지원되지 않는 토큰입니다.");
        } catch (MalformedJwtException ex) {
            throw new AuthException("JWT_MALFORMED", "잘못된 형식의 토큰입니다.");
        } catch (SignatureException ex) { // SecurityException is deprecated
            throw new AuthException("JWT_SIGNATURE_INVALID", "토큰 서명이 유효하지 않습니다.");
        } catch (IllegalArgumentException ex) {
            throw new AuthException("JWT_ILLEGAL_ARGUMENT", "토큰이 비어있거나 유효하지 않습니다.");
        }
    }
}