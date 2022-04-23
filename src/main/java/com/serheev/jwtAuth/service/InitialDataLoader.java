package com.serheev.jwtAuth.service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.serheev.jwtAuth.model.Role;
import com.serheev.jwtAuth.model.RoleName;
import com.serheev.jwtAuth.repository.RoleRepository;

@Component
public class InitialDataLoader {

    private final RoleRepository roleRepository;

    @Autowired
    public InitialDataLoader(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Bean
    public ApplicationRunner initializer() {
        List<RoleName> roles = Arrays.asList(RoleName.ADMIN, RoleName.DEVELOPER, RoleName.USER);
        return args -> roles.forEach(i -> createRoleIfNotFound(i));
    }

    private Optional<Role> createRoleIfNotFound(RoleName roleName) {
        Optional<Role> role = roleRepository.findByName(roleName);
        if (!role.isPresent()) {
            Role newRole = new Role();
            newRole.setName(roleName);
            roleRepository.save(newRole);
        }
        return role;
    }
}
