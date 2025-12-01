package org.tms.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.tms.audit.AuditService;
import org.tms.entity.UserEntity;
import org.tms.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuditService auditService;

    // View all users
    @GetMapping("/users")
    public List<UserEntity> getAllUsers(HttpServletRequest httpRequest) {
        List<UserEntity> users = userRepository.findAll();
        log.info("Admin fetched all users: count={}", users.size());
        auditService.audit("ADMIN_USERS_LISTED", null, "count=" + users.size());
        auditService.auditDb(
                "ADMIN_USERS_LISTED",
                null,
                null,
                users,
                httpRequest.getRequestURI(),
                httpRequest.getMethod(),
                null
        );
        return users;
    }

    // Delete a user
    @DeleteMapping("/users/{id}")
    public String deleteUser(@PathVariable Long id, HttpServletRequest httpRequest) {
        userRepository.deleteById(id);
        log.warn("Admin deleted user: id={}", id);
        auditService.audit("ADMIN_USER_DELETED", null, "id=" + id);
        auditService.auditDb(
                "ADMIN_USER_DELETED",
                null,
                id,
                "User deleted",
                httpRequest.getRequestURI(),
                httpRequest.getMethod(),
                null
        );
        return "User with ID " + id + " deleted successfully!";
    }

    // Change user role
    @PutMapping("/users/{id}/role")
    public String changeUserRole(@PathVariable Long id, @RequestParam String role, HttpServletRequest httpRequest) {
        UserEntity userEntity = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        userEntity.setRole(Enum.valueOf(org.tms.entity.Role.class, role.toUpperCase()));
        userRepository.save(userEntity);
        log.info("Admin changed user role: id={}, role={}", id, role);
        auditService.audit("ADMIN_USER_ROLE_CHANGED", null, "id=" + id + ", role=" + role);
        auditService.auditDb(
                "ADMIN_USER_ROLE_CHANGED",
                null,
                role,
                "Role updated",
                httpRequest.getRequestURI(),
                httpRequest.getMethod(),
                null
        );
        return "User role updated successfully!";
    }
}
