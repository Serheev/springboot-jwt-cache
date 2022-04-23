package com.serheev.jwtAuth.model;

import lombok.Data;

import javax.persistence.*;
import java.time.Instant;

@Data
@Entity
@Table(name = "refresh_token")
public class RefreshToken {

	@Id
    @Column
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "refresh_token_seq")
    @SequenceGenerator(name = "refresh_token_seq", allocationSize = 1)
    private Long id;

    @Column(name = "refresh_token", nullable = false, unique = true)
    private String refreshToken;

    @Column(name = "access_token", nullable = false, unique = true, length = 1024)
    private String accessToken;

    @OneToOne(optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "user_device_id", unique = true)
    private UserDevice userDevice;

    @Column(name = "refresh_count")
    private Long refreshCount;

    @Column(name = "expiry_dt", nullable = false)
    private Instant expiryDate;
    
    public void incrementRefreshCount() {
        refreshCount = refreshCount + 1;
    }
}
