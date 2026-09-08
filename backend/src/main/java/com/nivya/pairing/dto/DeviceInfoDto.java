package com.nivya.pairing.dto;

public class DeviceInfoDto {

    private String deviceUuid;
    private String deviceName;
    private String platform;
    private String osVersion;
    private String appVersion;
    private String pushToken;

    public DeviceInfoDto() {
    }

    public DeviceInfoDto(String deviceUuid, String deviceName, String platform, String osVersion, String appVersion, String pushToken) {
        this.deviceUuid = deviceUuid;
        this.deviceName = deviceName;
        this.platform = platform;
        this.osVersion = osVersion;
        this.appVersion = appVersion;
        this.pushToken = pushToken;
    }

    public String getDeviceUuid() {
        return deviceUuid;
    }

    public void setDeviceUuid(String deviceUuid) {
        this.deviceUuid = deviceUuid;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getPushToken() {
        return pushToken;
    }

    public void setPushToken(String pushToken) {
        this.pushToken = pushToken;
    }
}
