package com.nivya.history.entity;

import com.nivya.device.entity.Device;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA entity representing historical activity records exclusively accessible to Parent accounts.
 * Contains only legitimate metadata available within consented scope.
 */
@Entity
@Table(name = "history_events")
public class HistoryEvent {

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

    @Column(name = "activity_label", length = 255)
    private String activityLabel;

    @Column(nullable = false, length = 50)
    private String category = "GENERAL";

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "event_timestamp", nullable = false)
    private Instant eventTimestamp;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public HistoryEvent() {
    }

    public HistoryEvent(Device device, String packageName, String appName,
                        String broadActivity, String activityLabel, String category,
                        Integer durationSeconds, Instant eventTimestamp, String details) {
        this.device = device;
        this.packageName = packageName;
        this.appName = appName;
        this.broadActivity = broadActivity;
        this.activityLabel = activityLabel;
        this.category = category != null ? category : "GENERAL";
        this.durationSeconds = durationSeconds;
        this.eventTimestamp = eventTimestamp != null ? eventTimestamp : Instant.now();
        this.details = details;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.eventTimestamp == null) {
            this.eventTimestamp = Instant.now();
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

    public String getActivityLabel() {
        return activityLabel;
    }

    public void setActivityLabel(String activityLabel) {
        this.activityLabel = activityLabel;
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

    public Instant getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(Instant eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
