package org.telegram.admin.service;

import org.telegram.admin.dto.LoginRequest;
import org.telegram.admin.dto.LoginResponse;
import org.telegram.admin.model.Admin;
import org.telegram.admin.repository.AdminRepository;
import org.telegram.admin.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditLogService auditLogService;

    public AuthService(AdminRepository adminRepository, PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider, AuditLogService auditLogService) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.auditLogService = auditLogService;
    }

    public LoginResponse login(LoginRequest request, String ipAddress) {
        Admin admin = adminRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        if (!admin.isEnabled()) {
            throw new RuntimeException("Account is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        admin.setLastLogin(LocalDateTime.now());
        adminRepository.save(admin);

        String token = jwtTokenProvider.generateToken(admin.getUsername(), admin.getRole());

        auditLogService.log(admin.getUsername(), "LOGIN", "ADMIN", admin.getId().toString(),
                "Admin login successful", ipAddress);

        return new LoginResponse(token, admin.getUsername(), admin.getRole(), admin.getEmail());
    }

    public Admin createAdmin(String username, String password, String email, String role) {
        if (adminRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }
        Admin admin = new Admin();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setEmail(email);
        admin.setRole(role);
        return adminRepository.save(admin);
    }

    public Admin changePassword(String username, String oldPassword, String newPassword) {
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        if (!passwordEncoder.matches(oldPassword, admin.getPassword())) {
            throw new RuntimeException("Old password is incorrect");
        }
        admin.setPassword(passwordEncoder.encode(newPassword));
        return adminRepository.save(admin);
    }
}
