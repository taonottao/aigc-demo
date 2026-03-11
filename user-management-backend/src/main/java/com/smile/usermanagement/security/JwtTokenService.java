package com.smile.usermanagement.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.smile.usermanagement.config.JwtProperties;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenService {

    private final JwtProperties jwtProperties;
    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public JwtTokenService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.algorithm = Algorithm.HMAC256(jwtProperties.secret());
        this.verifier = JWT.require(algorithm).withIssuer(jwtProperties.issuer()).build();
    }

    public String issueToken(Long userId, String username) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtProperties.expireMinutes(), ChronoUnit.MINUTES);
        return JWT.create()
            .withIssuer(jwtProperties.issuer())
            .withIssuedAt(now)
            .withExpiresAt(expiresAt)
            .withClaim("uid", userId)
            .withClaim("username", username)
            .sign(algorithm);
    }

    public DecodedJWT verify(String token) {
        return verifier.verify(token);
    }
}

