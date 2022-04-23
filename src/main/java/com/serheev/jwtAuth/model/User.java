package com.serheev.jwtAuth.model;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "users", uniqueConstraints = { @UniqueConstraint(columnNames = { "email" }) })
public class User {

	@Id
    @Column
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
    private Long id;

    @Column
    @NotBlank(message = "User email cannot be null")
    private String email;

    @Column
    @NotNull(message = "Password cannot be null")
    private String password;

    @Column
    @NotBlank(message = "Name can not be blank")
    private String name;

    @Column(nullable = false)
    private Boolean active;
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles", 
      joinColumns = @JoinColumn(name = "user_id"), 
      inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();
    
    public void activate() {
		this.active = true;
	}
	
	public void deactivate() {
		this.active = false;
	}
}
