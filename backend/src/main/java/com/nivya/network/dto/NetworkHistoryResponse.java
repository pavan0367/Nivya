package com.nivya.network.dto;

import java.time.Instant;
import java.util.List;

public class NetworkHistoryResponse {

    private Long deviceId;
    private String deviceUuid;
    private List<NetworkPoint> points;

    public NetworkHistoryResponse() {
    }

    public NetworkHistoryResponse(Long deviceId, String deviceUuid, List<NetworkPoint> points) {
        this.deviceId = deviceId;
        this.deviceUuid = deviceUuid;
        this.points = points;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceUuid() {
        return deviceUuid;
    }

    public void setDeviceUuid(String deviceUuid) {
        this.deviceUuid = deviceUuid;
    }

    public List<NetworkPoint> getPoints() {
        return points;
    }

    public void setPoints(List<NetworkPoint> points) {
        this.points = points;
    }

    public static class NetworkPoint {
        private String networkType;
        private String connectionType;
        private Boolean isNetworkAvailable;
        private Boolean isInternetAvailable;
        private Integer signalLevel;
        private String quality;
        private Instant recordedAt;

        public NetworkPoint() {
        }

        public NetworkPoint(String networkType, String connectionType, Boolean isNetworkAvailable,
                            Boolean isInternetAvailable, Integer signalLevel, String quality,
                            Instant recordedAt) {
            this.networkType = networkType;
            this.connectionType = connectionType;
            this.isNetworkAvailable = isNetworkAvailable;
            this.isInternetAvailable = isInternetAvailable;
            this.signalLevel = signalLevel;
            this.quality = quality;
            this.recordedAt = recordedAt;
        }

        public String getNetworkType() {
            return networkType;
        }

        public void setNetworkType(String networkType) {
            this.networkType = networkType;
        }

        public String getConnectionType() {
            return connectionType;
        }

        public void setConnectionType(String connectionType) {
            this.connectionType = connectionType;
        }

        public Boolean getIsNetworkAvailable() {
            return isNetworkAvailable;
        }

        public void setIsNetworkAvailable(Boolean isNetworkAvailable) {
            this.isNetworkAvailable = isNetworkAvailable;
        }

        public Boolean getIsInternetAvailable() {
            return isInternetAvailable;
        }

        public void setIsInternetAvailable(Boolean isInternetAvailable) {
            this.isInternetAvailable = isInternetAvailable;
        }

        public Integer getSignalLevel() {
            return signalLevel;
        }

        public void setSignalLevel(Integer signalLevel) {
            this.signalLevel = signalLevel;
        }

        public String getQuality() {
            return quality;
        }

        public void setQuality(String quality) {
            this.quality = quality;
        }

        public Instant getRecordedAt() {
            return recordedAt;
        }

        public void setRecordedAt(Instant recordedAt) {
            this.recordedAt = recordedAt;
        }
    }
}
