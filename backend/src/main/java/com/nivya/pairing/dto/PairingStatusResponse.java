package com.nivya.pairing.dto;

import com.nivya.role.RoleType;

import java.util.ArrayList;
import java.util.List;

public class PairingStatusResponse {

    private boolean paired;
    private Long familyId;
    private String familyCode;
    private String familyName;
    private RoleType userRole;
    private List<LinkedMemberDto> members = new ArrayList<>();
    private List<DeviceStatusDto> devices = new ArrayList<>();

    public PairingStatusResponse() {
    }

    public PairingStatusResponse(boolean paired, Long familyId, String familyCode, String familyName,
                                 RoleType userRole, List<LinkedMemberDto> members, List<DeviceStatusDto> devices) {
        this.paired = paired;
        this.familyId = familyId;
        this.familyCode = familyCode;
        this.familyName = familyName;
        this.userRole = userRole;
        this.members = members != null ? members : new ArrayList<>();
        this.devices = devices != null ? devices : new ArrayList<>();
    }

    public static PairingStatusResponse unpaired(RoleType userRole) {
        PairingStatusResponse resp = new PairingStatusResponse();
        resp.setPaired(false);
        resp.setUserRole(userRole);
        return resp;
    }

    public boolean isPaired() {
        return paired;
    }

    public void setPaired(boolean paired) {
        this.paired = paired;
    }

    public Long getFamilyId() {
        return familyId;
    }

    public void setFamilyId(Long familyId) {
        this.familyId = familyId;
    }

    public String getFamilyCode() {
        return familyCode;
    }

    public void setFamilyCode(String familyCode) {
        this.familyCode = familyCode;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public RoleType getUserRole() {
        return userRole;
    }

    public void setUserRole(RoleType userRole) {
        this.userRole = userRole;
    }

    public List<LinkedMemberDto> getMembers() {
        return members;
    }

    public void setMembers(List<LinkedMemberDto> members) {
        this.members = members;
    }

    public List<DeviceStatusDto> getDevices() {
        return devices;
    }

    public void setDevices(List<DeviceStatusDto> devices) {
        this.devices = devices;
    }
}
