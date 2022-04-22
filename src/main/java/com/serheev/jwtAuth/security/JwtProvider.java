package com.serheev.jwtAuth.security;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import com.serheev.jwtAuth.cache.LoggedOutJwtTokenCache;
import com.serheev.jwtAuth.event.OnUserLogoutSuccessEvent;
import com.serheev.jwtAuth.exception.InvalidTokenRequestException;
import com.serheev.jwtAuth.model.User;
import com.serheev.jwtAuth.service.UserPrincipal;

@Component
public class JwtProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtProvider.class);

    @Value("${jwt.token.secret}")
    private String jwtSecret;
    @Value("${jwt.token.accessExpirationMs}")
    private Long jwtAccessTokenExpirationMs;
    @Value("${jwt.token.issuer}")
    private String jwtTokenIssuer;

    private LoggedOutJwtTokenCache loggedOutJwtTokenCache;

    @Autowired
    public void setLoggedOutJwtTokenCache(@Lazy LoggedOutJwtTokenCache loggedOutJwtTokenCache){
        this.loggedOutJwtTokenCache = loggedOutJwtTokenCache;
    }

    public String generateJwtToken(Authentication authentication) {

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtAccessTokenExpirationMs);

        Map<String, Object> roles = new HashMap<>();
        roles.put("roles", userPrincipal.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet()));

        return Jwts.builder()
                .setSubject((userPrincipal.getUsername()))
                .setIssuer(jwtTokenIssuer)
                .addClaims(roles)
                .setId(Long.toString(userPrincipal.getId()))
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
    }

    public String generateTokenFromUser(User user) {
        Instant expiryDate = Instant.now().plusMillis(jwtAccessTokenExpirationMs);
        return Jwts.builder()
                .setSubject(user.getEmail())
                .setIssuer(jwtTokenIssuer)
                .setId(Long.toString(user.getId()))
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(expiryDate))
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .getBody().getSubject();
    }

    public Date getTokenExpiryFromJWT(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .getBody();

        return claims.getExpiration();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken);
            validateTokenIsNotForALoggedOutDevice(authToken);
            return true;
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token -> Message: {}", e);
        } catch (ExpiredJwtException e) {
            logger.error("Expired JWT token -> Message: {}", e);
        } catch (UnsupportedJwtException e) {
            logger.error("Unsupported JWT token -> Message: {}", e);
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty -> Message: {}", e);
        }

        return false;
    }

    private void validateTokenIsNotForALoggedOutDevice(String authToken) {
        OnUserLogoutSuccessEvent previouslyLoggedOutEvent = loggedOutJwtTokenCache.getLogoutEventForToken(authToken);
        if (previouslyLoggedOutEvent != null) {
            String userEmail = previouslyLoggedOutEvent.getUserEmail();
            Date logoutEventDate = previouslyLoggedOutEvent.getEventTime();
            String errorMessage = String.format("Token corresponds to an already logged out user [%s] at [%s]. Please login again", userEmail, logoutEventDate);
            throw new InvalidTokenRequestException("JWT", authToken, errorMessage);
        }
    }

    public long getExpiryDuration() {
        return jwtAccessTokenExpirationMs;
    }
}
