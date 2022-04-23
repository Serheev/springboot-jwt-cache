package com.serheev.jwtAuth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.serheev.jwtAuth.model.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {

    @Override
    Optional<RefreshToken> findById(Integer id);

    Optional<RefreshToken> findByRefreshToken(String token);
}
