package com.serheev.jwtAuth.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.serheev.jwtAuth.exception.TokenRefreshException;
import com.serheev.jwtAuth.model.RefreshToken;
import com.serheev.jwtAuth.repository.RefreshTokenRepository;

@Service
public class RefreshTokenService {

    @Value("${jwt.token.refreshExpirationMs}")
    private Long jwtRefreshTokenExpirationMs;

    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByRefreshToken(token);
    }

    public RefreshToken save(RefreshToken refreshToken) {
        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken createRefreshToken() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setExpiryDate(Instant.now().plusMillis(jwtRefreshTokenExpirationMs));
        refreshToken.setRefreshToken(UUID.randomUUID().toString());
        refreshToken.setRefreshCount(0L);
        return refreshToken;
    }

    public void verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            throw new TokenRefreshException(token.getRefreshToken(), "Expired token. Please issue a new request");
        }
    }

    public void deleteById(Long id) {
        refreshTokenRepository.deleteById(id);
    }

    public void increaseCount(RefreshToken refreshToken) {
        refreshToken.incrementRefreshCount();
        save(refreshToken);
    }

    public void updateAccessToken(RefreshToken refreshToken) {
        refreshToken.setAccessToken(refreshToken.getAccessToken());
        save(refreshToken);
    }
}
