package com.serheev.jwtAuth.cache;

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.serheev.jwtAuth.event.OnUserLogoutSuccessEvent;
import com.serheev.jwtAuth.security.JwtProvider;

import net.jodah.expiringmap.ExpiringMap;

@Component
public class LoggedOutJwtTokenCache {
    private static final Logger logger = LoggerFactory.getLogger(LoggedOutJwtTokenCache.class);

    private ExpiringMap<String, OnUserLogoutSuccessEvent> tokenEventMap;
    private JwtProvider tokenProvider;

    @Autowired
    public void setTokenProvider(JwtProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Autowired
    public void setTokenEventMap() {
        this.tokenEventMap = ExpiringMap.builder()
                .variableExpiration()
                .maxSize(1000)
                .build();
    }

    public void markLogoutEventForToken(OnUserLogoutSuccessEvent event) {
        String token = event.getToken();
        if (tokenEventMap.containsKey(token)) {
            logger.info(String.format("Log out token for user [%s] is already present in the cache", event.getUserEmail()));

        } else {
            Date tokenExpiryDate = tokenProvider.getTokenExpiryFromJWT(token);
            long ttlForToken = getTTLForToken(tokenExpiryDate);
            logger.info(String.format("Logout token cache set for [%s] with a TTL of [%s] seconds. Token is due expiry at [%s]", event.getUserEmail(), ttlForToken, tokenExpiryDate));
            tokenEventMap.put(token, event, ttlForToken, TimeUnit.SECONDS);
        }
    }

    public OnUserLogoutSuccessEvent getLogoutEventForToken(String token) {
        return tokenEventMap.get(token);
    }

    private long getTTLForToken(Date date) {
        long secondAtExpiry = date.toInstant().getEpochSecond();
        long secondAtLogout = Instant.now().getEpochSecond();
        return Math.max(0, secondAtExpiry - secondAtLogout);
    }
}
