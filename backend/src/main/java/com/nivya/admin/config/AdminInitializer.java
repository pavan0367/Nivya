package com.nivya.admin.config;

import com.nivya.audit.service.AuditService;
import com.nivya.role.RoleType;
import com.nivya.user.entity.User;
import com.nivya.user.entity.UserStatus;
import com.nivya.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Idempotent Administrator Account Startup Provisioner.
 * Automatically provisions an initial ADMIN account if none exists, using
 * secure environment variables (NIVYA_ADMIN_EMAIL, NIVYA_ADMIN_PASSWORD, NIVYA_ADMIN_NAME).
 * Fails safely if environment variables are missing rather than using default insecure credentials.
 */
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Value("${nivya.admin.email:${NIVYA_ADMIN_EMAIL:}}")
    private String adminEmail;

    @Value("${nivya.admin.password:${NIVYA_ADMIN_PASSWORD:}}")
    private String adminPassword;

    @Value("${nivya.admin.name:${NIVYA_ADMIN_NAME:Administrator}}")
    private String adminName;

    public AdminInitializer(UserRepository userRepository,
                            PasswordEncoder passwordEncoder,
                            AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        provisionInitialAdmin();
    }

    /**
     * Idempotent check and provision method.
     */
    @Transactional
    public boolean provisionInitialAdmin() {
        if (userRepository.existsByRole(RoleType.ADMIN)) {
            log.info("Admin account already exists. Skipping administrator auto-provisioning.");
            return false;
        }

        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            log.info("No administrator account exists and NIVYA_ADMIN_EMAIL / NIVYA_ADMIN_PASSWORD environment variables are not configured. Safe bootstrap skipped.");
            return false;
        }

        String normalizedEmail = adminEmail.toLowerCase().trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            log.warn("Configured admin email {} already exists as a non-admin account. Promoting to ADMIN role.", normalizedEmail);
            User existing = userRepository.findByEmail(normalizedEmail).orElseThrow();
            existing.setRole(RoleType.ADMIN);
            existing.setStatus(UserStatus.ACTIVE);
            userRepository.save(existing);
            auditService.logEvent(existing.getId(), "ADMIN_PROVISIONED_PROMOTED", "Existing user promoted to initial administrator via startup configuration", "SYSTEM");
            return true;
        }

        User admin = new User(
                adminName != null && !adminName.isBlank() ? adminName.trim() : "Administrator",
                normalizedEmail,
                passwordEncoder.encode(adminPassword),
                RoleType.ADMIN
        );
        admin.setStatus(UserStatus.ACTIVE);
        admin = userRepository.save(admin);

        auditService.logEvent(admin.getId(), "ADMIN_PROVISIONED", "Initial system administrator created via startup configuration", "SYSTEM");
        log.info("Successfully provisioned initial administrator account: {}", admin.getEmail());
        return true;
    }
}
