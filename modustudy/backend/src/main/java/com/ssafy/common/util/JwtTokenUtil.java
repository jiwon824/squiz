package com.ssafy.common.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.*;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtTokenUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenUtil.class);

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-validity}")
    private long accessTokenValidity;

    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";

    public String getToken(String userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenValidity);

        return JWT.create()
                .withSubject(userId)
                .withIssuedAt(now)
                .withExpiresAt(expiry)
                .sign(Algorithm.HMAC512(secretKey.getBytes()));
    }

    public String getUserId(String token) {
        DecodedJWT decodedJWT = JWT.decode(token);
        return decodedJWT.getSubject();
    }

    public boolean validateToken(String token) {
        try {
            JWTVerifier verifier = JWT.require(Algorithm.HMAC512(secretKey.getBytes())).build();
            verifier.verify(token);
            return true;
        } catch (AlgorithmMismatchException e) {
} catch (TokenExpiredException e) {
} catch (SignatureVerificationException e) {
} catch (JWTDecodeException e) {
} catch (JWTVerificationException e) {
}
        return false;
    }
    /**
     * Access Token 생성 (OAuth용 별칭)
     */
    public String createAccessToken(String userId) {
        return getToken(userId);  // 기존 메서드 활용
    }

    /**
     * Refresh Token 생성
     */
    public String createRefreshToken(String userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + (accessTokenValidity * 7));  // 7배 길게

        return JWT.create()
                .withSubject(userId)
                .withIssuedAt(now)
                .withExpiresAt(expiry)
                .withClaim("type", "refresh")
                .sign(Algorithm.HMAC512(secretKey.getBytes()));
    }
}

