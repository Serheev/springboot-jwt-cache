package com.serheev.jwtAuth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.serheev.jwtAuth.model.RefreshToken;
import com.serheev.jwtAuth.model.UserDevice;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Integer> {

    @Override
    Optional<UserDevice> findById(Integer id);

    Optional<UserDevice> findByRefreshToken(RefreshToken refreshToken);

    Optional<UserDevice> findByUserId(Integer userId);
}
