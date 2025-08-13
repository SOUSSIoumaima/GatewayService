package com.hsurveys.gateway.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Component
public class JwtUtil {
    private final String secret;
    private final long expiration;
    private final SecretKey signingKey;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration}") long expiration) {
        this.secret = secret;
        this.expiration = expiration;
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }



    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public UUID extractUserId(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            String userId = (String) claims.get("userId");
            return userId != null ? UUID.fromString(userId) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public UUID extractOrganizationId(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            String orgId = (String) claims.get("organizationId");
            return orgId != null ? UUID.fromString(orgId) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public UUID extractDepartmentId(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            String departmentId = (String) claims.get("departmentId");
            return departmentId != null ? UUID.fromString(departmentId) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public UUID extractTeamId(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            String teamId = (String) claims.get("teamId");
            return teamId != null ? UUID.fromString(teamId) : null;
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> extractAuthorities(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            List<String> authorities = (List<String>) claims.get("authorities");
            return authorities != null ? authorities : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            List<String> roles = (List<String>) claims.get("roles");
            return roles != null ? roles : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Boolean validateToken(String token) {
        try {

            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Boolean hasOrganizationId(String token) {
        UUID orgId = extractOrganizationId(token);
        return orgId != null;
    }

    public Boolean hasUserId(String token) {
        UUID userId = extractUserId(token);
        return userId != null;
    }

    public Boolean hasDepartmentId(String token) {
        UUID departmentId = extractDepartmentId(token);
        return departmentId != null;
    }

    public Boolean hasTeamId(String token) {
        UUID teamId = extractTeamId(token);
        return teamId != null;
    }
}