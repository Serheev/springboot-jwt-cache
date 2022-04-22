package com.serheev.jwtAuth.controller;

import java.net.URI;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.validation.Valid;

import com.serheev.jwtAuth.dto.LogOutRequest;
import com.serheev.jwtAuth.event.OnUserLogoutSuccessEvent;
import io.jsonwebtoken.ExpiredJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.serheev.jwtAuth.dto.LoginForm;
import com.serheev.jwtAuth.dto.SignUpForm;
import com.serheev.jwtAuth.dto.TokenRefreshRequest;
import com.serheev.jwtAuth.exception.TokenRefreshException;
import com.serheev.jwtAuth.model.RefreshToken;
import com.serheev.jwtAuth.model.Role;
import com.serheev.jwtAuth.model.RoleName;
import com.serheev.jwtAuth.model.User;
import com.serheev.jwtAuth.model.UserDevice;
import com.serheev.jwtAuth.repository.RoleRepository;
import com.serheev.jwtAuth.repository.UserRepository;
import com.serheev.jwtAuth.response.ApiResponse;
import com.serheev.jwtAuth.response.JwtResponse;
import com.serheev.jwtAuth.response.UserIdentityAvailability;
import com.serheev.jwtAuth.security.JwtProvider;
import com.serheev.jwtAuth.service.RefreshTokenService;
import com.serheev.jwtAuth.service.UserDeviceService;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private UserDeviceService userDeviceService;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginForm loginRequest) {

        User user = userRepository.findByEmail(loginRequest.getEmail()).orElseThrow(() -> new RuntimeException("Fail! -> Cause: User not found."));

        if (user.getActive()) {
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwtToken = jwtProvider.generateJwtToken(authentication);

            // The actual access-token must be placed to cached lock-list on re-authentication
            userDeviceService.findByUserId(user.getId()).map(UserDevice::getRefreshToken).map(RefreshToken::getAccessToken).ifPresent(token -> {
                // When the token has expired in the database, an ExpiredJwtException will be thrown
                try {
                    applicationEventPublisher.publishEvent(new OnUserLogoutSuccessEvent(user.getEmail(), token, new LogOutRequest()));
                } catch (ExpiredJwtException exception) {
                    logger.error(String.format("Access Token [%s] is expired", token));
                }
            });

            // It is required to delete the actual user refresh-token from the database before it is updated
            userDeviceService.findByUserId(user.getId())
                    .map(UserDevice::getRefreshToken)
                    .map(RefreshToken::getId)
                    .ifPresent(refreshTokenService::deleteById);

            UserDevice userDevice = userDeviceService.createUserDevice(loginRequest.getDeviceInfo());
            RefreshToken refreshToken = refreshTokenService.createRefreshToken();
            userDevice.setUser(user);
            userDevice.setRefreshToken(refreshToken);
            refreshToken.setUserDevice(userDevice);
            refreshToken.setAccessToken(jwtToken);
            refreshToken = refreshTokenService.save(refreshToken);

            return ResponseEntity.ok(new JwtResponse(jwtToken, refreshToken.getRefreshToken(), jwtProvider.getExpiryDuration()));
        }
        return ResponseEntity.badRequest().body(new ApiResponse(false, "User has been deactivated/locked !!"));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpForm signUpRequest) {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return new ResponseEntity<String>("Fail -> Email is already in use!", HttpStatus.BAD_REQUEST);
        }

        User user = new User();
        user.setName(signUpRequest.getName());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(encoder.encode(signUpRequest.getPassword()));

        Set<String> strRoles = signUpRequest.getRole();
        Set<Role> roles = new HashSet<>();

        strRoles.forEach(role -> {
            switch (role) {
                case "admin":
                    Role adminRole = roleRepository.findByName(RoleName.ADMIN).orElseThrow(() -> new RuntimeException("Fail! -> Cause: User Role not found."));
                    roles.add(adminRole);
                    break;
                case "developer":
                    Role therapistRole = roleRepository.findByName(RoleName.DEVELOPER).orElseThrow(() -> new RuntimeException("Fail! -> Cause: User Role not found."));
                    roles.add(therapistRole);
                    break;
                default:
                    Role userRole = roleRepository.findByName(RoleName.USER).orElseThrow(() -> new RuntimeException("Fail! -> Cause: User Role not found."));
                    roles.add(userRole);
            }
        });

        user.setRoles(roles);
        user.activate();
        User result = userRepository.save(user);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath().path("/user/me").buildAndExpand(result.getId()).toUri();

        return ResponseEntity.created(location).body(new ApiResponse(true, "User registered successfully!"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshJwtToken(@Valid @RequestBody TokenRefreshRequest tokenRefreshRequest) {

        String requestRefreshToken = tokenRefreshRequest.getRefreshToken();

        Optional<String> token = Optional.of(refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshToken -> {
                    refreshTokenService.verifyExpiration(refreshToken);
                    userDeviceService.verifyRefreshAvailability(refreshToken);
                    refreshTokenService.increaseCount(refreshToken);
                    // The actual access-token must be placed to cached lock-list on refresh
                    if (jwtProvider.validateJwtToken(refreshToken.getAccessToken())) {
                        applicationEventPublisher.publishEvent(new OnUserLogoutSuccessEvent(refreshToken.getUserDevice().getUser().getEmail(), refreshToken.getAccessToken(), new LogOutRequest()));
                    }
                    return refreshToken;
                })
                .map(RefreshToken::getUserDevice)
                .map(UserDevice::getUser)
                .map(u -> jwtProvider.generateTokenFromUser(u))
                .orElseThrow(() -> new TokenRefreshException(requestRefreshToken, "Missing refresh token in database.Please login again")));

        // Access-token must be updated in database after refresh
        refreshTokenService.findByToken(requestRefreshToken)
                .ifPresent(r -> {
                    r.setAccessToken(token.get());
                    refreshTokenService.updateAccessToken(r);
                });
        return ResponseEntity.ok().body(new JwtResponse(token.get(), tokenRefreshRequest.getRefreshToken(), jwtProvider.getExpiryDuration()));
    }

    @GetMapping("/checkEmailAvailability")
    public UserIdentityAvailability checkEmailAvailability(@RequestParam(value = "email") String email) {
        return new UserIdentityAvailability(!userRepository.existsByEmail(email));
    }

}
