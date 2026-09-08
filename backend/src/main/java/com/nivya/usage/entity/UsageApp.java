package com.nivya.usage.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * JPA entity representing per-application usage duration, category, and last used time.
 */
@Entity
@Table(name = "usage_apps")
public class UsageApp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "summary_id", nullable = false)
    private UsageSummary summary;

    @Column(name = "package_name", nullable = false, length = 150)
    private String packageName;

    @Column(name = "app_name", nullable = false, length = 100)
    private String appName;

    @Column(nullable = false, length = 50)
    private String category = "OTHER";

    @Column(name = "foreground_seconds", nullable = false)
    private Long foregroundSeconds = 0L;

    @Column(name = "last_time_used")
    private Instant lastTimeUsed;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public UsageApp() {
    }

    public UsageApp(UsageSummary summary, String packageName, String appName,
                    String category, Long foregroundSeconds, Instant lastTimeUsed) {
        this.summary = summary;
        this.packageName = packageName;
        this.appName = appName;
        this.category = category != null ? category : "OTHER";
        this.foregroundSeconds = foregroundSeconds != null ? foregroundSeconds : 0L;
        this.lastTimeUsed = lastTimeUsed;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UsageSummary getSummary() {
        return summary;
    }

    public void setSummary(UsageSummary summary) {
        this.summary = summary;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Long getForegroundSeconds() {
        return foregroundSeconds;
    }

    public void setForegroundSeconds(Long foregroundSeconds) {
        this.foregroundSeconds = foregroundSeconds;
    }

    public Instant getLastTimeUsed() {
        return lastTimeUsed;
    }

    public void setLastTimeUsed(Instant lastTimeUsed) {
        this.lastTimeUsed = lastTimeUsed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
