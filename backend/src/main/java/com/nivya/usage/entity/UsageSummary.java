package com.nivya.usage.entity;

import com.nivya.device.entity.Device;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity representing daily aggregated screen time and category breakdown for a device.
 */
@Entity
@Table(name = "usage_summary")
public class UsageSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "total_foreground_seconds", nullable = false)
    private Long totalForegroundSeconds = 0L;

    @Column(name = "screen_unlocks", nullable = false)
    private Integer screenUnlocks = 0;

    @Column(name = "educational_seconds", nullable = false)
    private Long educationalSeconds = 0L;

    @Column(name = "recreational_seconds", nullable = false)
    private Long recreationalSeconds = 0L;

    @Column(name = "social_seconds", nullable = false)
    private Long socialSeconds = 0L;

    @Column(name = "productivity_seconds", nullable = false)
    private Long productivitySeconds = 0L;

    @OneToMany(mappedBy = "summary", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UsageApp> apps = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public UsageSummary() {
    }

    public UsageSummary(Device device, LocalDate date, Long totalForegroundSeconds,
                        Integer screenUnlocks, Long educationalSeconds,
                        Long recreationalSeconds, Long socialSeconds, Long productivitySeconds) {
        this.device = device;
        this.date = date;
        this.totalForegroundSeconds = totalForegroundSeconds != null ? totalForegroundSeconds : 0L;
        this.screenUnlocks = screenUnlocks != null ? screenUnlocks : 0;
        this.educationalSeconds = educationalSeconds != null ? educationalSeconds : 0L;
        this.recreationalSeconds = recreationalSeconds != null ? recreationalSeconds : 0L;
        this.socialSeconds = socialSeconds != null ? socialSeconds : 0L;
        this.productivitySeconds = productivitySeconds != null ? productivitySeconds : 0L;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    @PreUpdate
    protected void onPersistOrUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Long getTotalForegroundSeconds() {
        return totalForegroundSeconds;
    }

    public void setTotalForegroundSeconds(Long totalForegroundSeconds) {
        this.totalForegroundSeconds = totalForegroundSeconds;
    }

    public Integer getScreenUnlocks() {
        return screenUnlocks;
    }

    public void setScreenUnlocks(Integer screenUnlocks) {
        this.screenUnlocks = screenUnlocks;
    }

    public Long getEducationalSeconds() {
        return educationalSeconds;
    }

    public void setEducationalSeconds(Long educationalSeconds) {
        this.educationalSeconds = educationalSeconds;
    }

    public Long getRecreationalSeconds() {
        return recreationalSeconds;
    }

    public void setRecreationalSeconds(Long recreationalSeconds) {
        this.recreationalSeconds = recreationalSeconds;
    }

    public Long getSocialSeconds() {
        return socialSeconds;
    }

    public void setSocialSeconds(Long socialSeconds) {
        this.socialSeconds = socialSeconds;
    }

    public Long getProductivitySeconds() {
        return productivitySeconds;
    }

    public void setProductivitySeconds(Long productivitySeconds) {
        this.productivitySeconds = productivitySeconds;
    }

    public List<UsageApp> getApps() {
        return apps;
    }

    public void setApps(List<UsageApp> apps) {
        this.apps = apps;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
