package com.nivya.activity.entity;

import com.nivya.device.entity.Device;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA entity representing Parent-only high-level activity telemetry events.
 * Captures legitimate application context without accessing private message
 * bodies, credentials, call audio, or secure app contents.
 */
@Entity
@Table(name = "activity_events")
public class ActivityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "package_name", nullable = false, length = 150)
    private String packageName;

    @Column(name = "app_name", nullable = false, length = 150)
    private String appName;

    @Column(name = "broad_activity", nullable = false, length = 255)
    private String broadActivity;

    @Column(nullable = false, length = 50)
    private String category = "GENERAL";

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "is_current", nullable = false)
    private boolean current = true;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ActivityEvent() {
    }

    public ActivityEvent(Device device, String packageName, String appName,
                         String broadActivity, String category, Integer durationSeconds,
                         boolean current, Instant startedAt, Instant endedAt) {
        this.device = device;
        this.packageName = packageName;
        this.appName = appName;
        this.broadActivity = broadActivity;
        this.category = category != null ? category : "GENERAL";
        this.durationSeconds = durationSeconds;
        this.current = current;
        this.startedAt = startedAt != null ? startedAt : Instant.now();
        this.endedAt = endedAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.startedAt == null) {
            this.startedAt = Instant.now();
        }
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

    public String getBroadActivity() {
        return broadActivity;
    }

    public void setBroadActivity(String broadActivity) {
        this.broadActivity = broadActivity;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public boolean isCurrent() {
        return current;
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
