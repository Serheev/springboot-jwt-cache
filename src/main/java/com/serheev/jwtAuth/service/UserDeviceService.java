package com.serheev.jwtAuth.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.serheev.jwtAuth.dto.DeviceInfo;
import com.serheev.jwtAuth.exception.TokenRefreshException;
import com.serheev.jwtAuth.model.RefreshToken;
import com.serheev.jwtAuth.model.UserDevice;
import com.serheev.jwtAuth.repository.UserDeviceRepository;

@Service
public class UserDeviceService {

    private final UserDeviceRepository userDeviceRepository;

    @Autowired
    public UserDeviceService(UserDeviceRepository userDeviceRepository) {
        this.userDeviceRepository = userDeviceRepository;
    }

    public Optional<UserDevice> findByUserId(int userId) {
        return userDeviceRepository.findByUserId(userId);
    }

    public Optional<UserDevice> findByRefreshToken(RefreshToken refreshToken) {
        return userDeviceRepository.findByRefreshToken(refreshToken);
    }

    public UserDevice createUserDevice(DeviceInfo deviceInfo) {
        UserDevice userDevice = new UserDevice();
        userDevice.setDeviceId(deviceInfo.getDeviceId());
        userDevice.setDeviceType(deviceInfo.getDeviceType());
        userDevice.setIsRefreshActive(true);
        return userDevice;
    }

    public void verifyRefreshAvailability(RefreshToken refreshToken) {
        UserDevice userDevice = findByRefreshToken(refreshToken)
                .orElseThrow(() -> new TokenRefreshException(refreshToken.getRefreshToken(), "No device found for the matching token. Please login again"));

        if (!userDevice.getIsRefreshActive()) {
            throw new TokenRefreshException(refreshToken.getRefreshToken(), "Refresh blocked for the device. Please login through a different device");
        }
    }
}
