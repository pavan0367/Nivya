package com.nivya.role.dto;

import com.nivya.role.RoleType;

import java.util.List;

/**
 * Response DTO providing authoritative role metadata, permitted modules, and navigation targets.
 */
public class RoleInfoResponse {

    private RoleType role;
    private String roleName;
    private String description;
    private String nextScreen = "CONNECTION_SCREEN";
    private List<String> availableModules;
    private List<String> prohibitedModules;

    public RoleInfoResponse() {
    }

    public RoleInfoResponse(RoleType role, String description, List<String> availableModules, List<String> prohibitedModules) {
        this.role = role;
        this.roleName = role.name();
        this.description = description;
        this.availableModules = availableModules;
        this.prohibitedModules = prohibitedModules;
    }

    public static RoleInfoResponse forRole(RoleType role) {
        if (role == RoleType.PARENT) {
            return new RoleInfoResponse(
                    RoleType.PARENT,
                    "Parent role with full device-status monitoring, historical telemetry, and guidance controls.",
                    List.of(
                            "Dashboard",
                            "LiveActivity",
                            "History",
                            "AppUsage",
                            "Communication",
                            "Location",
                            "DeviceHealth",
                            "Alerts",
                            "Convocation",
                            "FamilyDevices",
                            "Settings"
                    ),
                    List.of() // Parents have access to parent suite
            );
        } else {
            return new RoleInfoResponse(
                    RoleType.CHILD,
                    "Child role focused on personal device care, screen time awareness, and isolated convocation.",
                    List.of(
                            "Dashboard",
                            "Battery",
                            "ScreenTime",
                            "Network",
                            "Location",
                            "DeviceHealth",
                            "CleanUp",
                            "Alerts",
                            "Convocation",
                            "Settings"
                    ),
                    List.of(
                            "LiveActivity",
                            "History",
                            "FamilyDevices",
                            "ParentSettings"
                    ) // Strictly prohibited and omitted from Child UI
            );
        }
    }

    public RoleType getRole() {
        return role;
    }

    public void setRole(RoleType role) {
        this.role = role;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNextScreen() {
        return nextScreen;
    }

    public void setNextScreen(String nextScreen) {
        this.nextScreen = nextScreen;
    }

    public List<String> getAvailableModules() {
        return availableModules;
    }

    public void setAvailableModules(List<String> availableModules) {
        this.availableModules = availableModules;
    }

    public List<String> getProhibitedModules() {
        return prohibitedModules;
    }

    public void setProhibitedModules(List<String> prohibitedModules) {
        this.prohibitedModules = prohibitedModules;
    }
}
