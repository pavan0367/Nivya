package com.nivya.admin.dto;

/**
 * Administrative family association summary.
 */
public class AdminFamilyDto {

    private Long familyId;
    private String familyCode;
    private String familyName;
    private int memberCount;
    private String roleInFamily;

    public AdminFamilyDto() {
    }

    public AdminFamilyDto(Long familyId, String familyCode, String familyName, int memberCount, String roleInFamily) {
        this.familyId = familyId;
        this.familyCode = familyCode;
        this.familyName = familyName;
        this.memberCount = memberCount;
        this.roleInFamily = roleInFamily;
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

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public String getRoleInFamily() {
        return roleInFamily;
    }

    public void setRoleInFamily(String roleInFamily) {
        this.roleInFamily = roleInFamily;
    }
}
