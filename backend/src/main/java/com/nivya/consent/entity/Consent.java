package com.nivya.consent.entity;

import com.nivya.family.entity.Family;
import com.nivya.user.entity.User;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * Explicit consent record logging family safety monitoring agreements.
 */
@Entity
@Table(name = "consents")
public class Consent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @Column(name = "terms_version", nullable = false, length = 20)
    private String termsVersion = "1.0";

    @Column(name = "monitoring_consent", nullable = false)
    private boolean monitoringConsent = true;

    @Column(name = "location_consent", nullable = false)
    private boolean locationConsent = true;

    @Column(name = "accepted_at", nullable = false, updatable = false)
    private Instant acceptedAt;

    public Consent() {
    }

    public Consent(User user, Family family, String termsVersion, boolean monitoringConsent, boolean locationConsent) {
        this.user = user;
        this.family = family;
        this.termsVersion = termsVersion != null ? termsVersion : "1.0";
        this.monitoringConsent = monitoringConsent;
        this.locationConsent = locationConsent;
    }

    @PrePersist
    protected void onCreate() {
        if (this.acceptedAt == null) {
            this.acceptedAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Family getFamily() {
        return family;
    }

    public void setFamily(Family family) {
        this.family = family;
    }

    public String getTermsVersion() {
        return termsVersion;
    }

    public void setTermsVersion(String termsVersion) {
        this.termsVersion = termsVersion;
    }

    public boolean isMonitoringConsent() {
        return monitoringConsent;
    }

    public void setMonitoringConsent(boolean monitoringConsent) {
        this.monitoringConsent = monitoringConsent;
    }

    public boolean isLocationConsent() {
        return locationConsent;
    }

    public void setLocationConsent(boolean locationConsent) {
        this.locationConsent = locationConsent;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(Instant acceptedAt) {
        this.acceptedAt = acceptedAt;
    }
}
