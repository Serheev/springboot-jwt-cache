package com.serheev.jwtAuth.model;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@Table(name = "roles")
public class Role extends BaseEntity implements Serializable {
    @Enumerated(EnumType.STRING)
    @Column(length = 60)
    private RoleName name;
}
