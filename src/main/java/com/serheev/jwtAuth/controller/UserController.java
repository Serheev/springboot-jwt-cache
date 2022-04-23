package com.serheev.jwtAuth.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.serheev.jwtAuth.dto.LogOutRequest;
import com.serheev.jwtAuth.event.OnUserLogoutSuccessEvent;
import com.serheev.jwtAuth.exception.ResourceNotFoundException;
import com.serheev.jwtAuth.exception.UserLogoutException;
import com.serheev.jwtAuth.model.User;
import com.serheev.jwtAuth.model.UserDevice;
import com.serheev.jwtAuth.repository.UserRepository;
import com.serheev.jwtAuth.response.ApiResponse;
import com.serheev.jwtAuth.response.UserProfile;
import com.serheev.jwtAuth.service.CurrentUser;
import com.serheev.jwtAuth.service.RefreshTokenService;
import com.serheev.jwtAuth.service.UserDeviceService;
import com.serheev.jwtAuth.service.UserPrincipal;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Setter(onMethod=@__({@Autowired}))
    private UserRepository userRepository;

    @Setter(onMethod=@__({@Autowired}))
    private RefreshTokenService refreshTokenService;

    @Setter(onMethod=@__({@Autowired}))
    private UserDeviceService userDeviceService;

    @Setter(onMethod=@__({@Autowired}))
    private ApplicationEventPublisher applicationEventPublisher;

    @GetMapping("/me")
    public User getCurrentUser(@CurrentUser UserPrincipal userPrincipal) {
        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userPrincipal.getId()));
    }

    @GetMapping()
    @PreAuthorize("hasAuthority('ADMIN')")
    public List<UserProfile> getUserProfile(@RequestParam(value = "email", required = false) Optional<String> email) {
    	List<UserProfile> userProfiles = new ArrayList<>();
    	if (email.isPresent()) {
    		User user = userRepository.findByEmail(email.get())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "email", email.get()));
    		UserProfile userProfile = new UserProfile(user.getId(), user.getEmail(), user.getName(),  user.getActive());
    		userProfiles.add(userProfile);
    	} else {
    		List<User> users = userRepository.findAll();
    		for (User u: users) {
    			UserProfile userProfile = new UserProfile(u.getId(), u.getEmail(), u.getName(),  u.getActive());
    			userProfiles.add(userProfile);
    		}
    	}
        return userProfiles;
    }

    @GetMapping("/byID/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public UserProfile getUserProfileById(@PathVariable(value = "id") int id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        UserProfile userProfile = new UserProfile(user.getId(), user.getEmail(), user.getName(), user.getActive());

        return userProfile;
    }

    @PutMapping("/byID/{id}/deactivate")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse> deactivateUserById(@PathVariable(value = "id") int id) {
    	User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.deactivate();
        userRepository.save(user);
        return ResponseEntity.ok(new ApiResponse(true, "User deactivated successfully!"));  
    }

    @PutMapping("/byID/{id}/activate")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse> activateUserById(@PathVariable(value = "id") int id) {
       User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.activate();
        userRepository.save(user);
        return ResponseEntity.ok(new ApiResponse(true, "User activated successfully!"));
    }

    @DeleteMapping("/byID/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse> deleteUser(@PathVariable(value = "id") int id) {
    	User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        userRepository.delete(user);
        return ResponseEntity.ok(new ApiResponse(true, "User deleted successfully!"));
    }
        
    @PutMapping("/logout")
    public ResponseEntity<ApiResponse> logoutUser(@CurrentUser UserPrincipal currentUser,
    		@Valid @RequestBody LogOutRequest logOutRequest) {
        String deviceId = logOutRequest.getDeviceInfo().getDeviceId();
        UserDevice userDevice = userDeviceService.findByUserId(currentUser.getId())
                .filter(device -> device.getDeviceId().equals(deviceId))
                .orElseThrow(() -> new UserLogoutException(logOutRequest.getDeviceInfo().getDeviceId(), "Invalid device Id supplied. No matching device found for the given user "));
        refreshTokenService.deleteById(userDevice.getRefreshToken().getId());
        
        OnUserLogoutSuccessEvent logoutSuccessEvent = new OnUserLogoutSuccessEvent(currentUser.getEmail(), logOutRequest.getToken(), logOutRequest);
        applicationEventPublisher.publishEvent(logoutSuccessEvent);
        return ResponseEntity.ok(new ApiResponse(true, "User has successfully logged out from the system!"));
    }

}
