package com.serheev.jwtAuth.model;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name ="user_device")
public class UserDevice {

	@Id
    @Column
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_device_seq")
    @SequenceGenerator(name = "user_device_seq", allocationSize = 1)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "device_type")
    private String deviceType;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @OneToOne(optional = false, mappedBy = "userDevice")
    private RefreshToken refreshToken;

    @Column(name = "is_refresh_active")
    private Boolean isRefreshActive;
}
