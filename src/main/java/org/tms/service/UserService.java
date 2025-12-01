package org.tms.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.tms.entity.Role;
import org.tms.entity.UserEntity;
import org.tms.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Load user by username for authentication (used by Spring Security)
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("UserService loading user by username: {}", username);
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found with username: " + username));

        return User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().name())  // Role Enum (ADMIN/USER)
                .build();
    }

    /**
     * Register new user (default role: USER)
     */
    public UserEntity registerUser(String username, String password) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.USER);

        UserEntity saved = userRepository.save(user);
        log.info("Registered USER: {}", saved.getUsername());
        return saved;
    }

    /**
     * Register new admin (for initial setup or restricted API)
     */
    public UserEntity registerAdmin(String username, String password) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        UserEntity admin = new UserEntity();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(Role.ADMIN);

        UserEntity saved = userRepository.save(admin);
        log.info("Registered ADMIN: {}", saved.getUsername());
        return saved;
    }

    /**
     * Fetch all users — should be accessible only by Admin
     */
    public List<UserEntity> getAllUsers() {
        List<UserEntity> users = userRepository.findAll();
        log.info("Fetched all users: count={}", users.size());
        return users;
    }
}
