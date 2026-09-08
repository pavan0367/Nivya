package com.nivya.pairing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ConnectPairingRequest {

    @NotBlank(message = "Pairing code cannot be blank")
    @Pattern(regexp = "^(NV-)?[A-Za-z0-9]{4}-?[A-Za-z0-9]{4}$", message = "Pairing code must match format NV-XXXX-XXXX or XXXX-XXXX")
    private String code;

    private DeviceInfoDto deviceInfo;

    public ConnectPairingRequest() {
    }

    public ConnectPairingRequest(String code, DeviceInfoDto deviceInfo) {
        this.code = code;
        this.deviceInfo = deviceInfo;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public DeviceInfoDto getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(DeviceInfoDto deviceInfo) {
        this.deviceInfo = deviceInfo;
    }
}
