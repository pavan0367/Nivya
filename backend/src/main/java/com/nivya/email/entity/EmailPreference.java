package com.nivya.email.entity;

import com.nivya.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "email_preferences")
public class EmailPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "login_alerts_enabled", nullable = false)
    private boolean loginAlertsEnabled = true;

    @Column(name = "new_device_alerts_enabled", nullable = false)
    private boolean newDeviceAlertsEnabled = true;

    @Column(name = "app_updates_enabled", nullable = false)
    private boolean appUpdatesEnabled = true;

    @Column(name = "security_critical_enabled", nullable = false)
    private boolean securityCriticalEnabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public EmailPreference() {
    }

    public EmailPreference(User user) {
        this.user = user;
        this.loginAlertsEnabled = true;
        this.newDeviceAlertsEnabled = true;
        this.appUpdatesEnabled = true;
        this.securityCriticalEnabled = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public boolean isLoginAlertsEnabled() { return loginAlertsEnabled; }
    public void setLoginAlertsEnabled(boolean loginAlertsEnabled) { this.loginAlertsEnabled = loginAlertsEnabled; }

    public boolean isNewDeviceAlertsEnabled() { return newDeviceAlertsEnabled; }
    public void setNewDeviceAlertsEnabled(boolean newDeviceAlertsEnabled) { this.newDeviceAlertsEnabled = newDeviceAlertsEnabled; }

    public boolean isAppUpdatesEnabled() { return appUpdatesEnabled; }
    public void setAppUpdatesEnabled(boolean appUpdatesEnabled) { this.appUpdatesEnabled = appUpdatesEnabled; }

    public boolean isSecurityCriticalEnabled() { return securityCriticalEnabled; }
    public void setSecurityCriticalEnabled(boolean securityCriticalEnabled) { this.securityCriticalEnabled = securityCriticalEnabled; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
