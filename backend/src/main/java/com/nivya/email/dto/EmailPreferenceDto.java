package com.nivya.email.dto;

import com.nivya.email.entity.EmailPreference;

public class EmailPreferenceDto {

    private boolean loginAlertsEnabled;
    private boolean newDeviceAlertsEnabled;
    private boolean appUpdatesEnabled;
    private boolean securityCriticalEnabled;

    public EmailPreferenceDto() {
    }

    public EmailPreferenceDto(boolean loginAlertsEnabled, boolean newDeviceAlertsEnabled, boolean appUpdatesEnabled, boolean securityCriticalEnabled) {
        this.loginAlertsEnabled = loginAlertsEnabled;
        this.newDeviceAlertsEnabled = newDeviceAlertsEnabled;
        this.appUpdatesEnabled = appUpdatesEnabled;
        this.securityCriticalEnabled = securityCriticalEnabled;
    }

    public static EmailPreferenceDto fromEntity(EmailPreference entity) {
        if (entity == null) {
            return new EmailPreferenceDto(true, true, true, true);
        }
        return new EmailPreferenceDto(
                entity.isLoginAlertsEnabled(),
                entity.isNewDeviceAlertsEnabled(),
                entity.isAppUpdatesEnabled(),
                entity.isSecurityCriticalEnabled()
        );
    }

    public boolean isLoginAlertsEnabled() { return loginAlertsEnabled; }
    public void setLoginAlertsEnabled(boolean loginAlertsEnabled) { this.loginAlertsEnabled = loginAlertsEnabled; }

    public boolean isNewDeviceAlertsEnabled() { return newDeviceAlertsEnabled; }
    public void setNewDeviceAlertsEnabled(boolean newDeviceAlertsEnabled) { this.newDeviceAlertsEnabled = newDeviceAlertsEnabled; }

    public boolean isAppUpdatesEnabled() { return appUpdatesEnabled; }
    public void setAppUpdatesEnabled(boolean appUpdatesEnabled) { this.appUpdatesEnabled = appUpdatesEnabled; }

    public boolean isSecurityCriticalEnabled() { return securityCriticalEnabled; }
    public void setSecurityCriticalEnabled(boolean securityCriticalEnabled) { this.securityCriticalEnabled = securityCriticalEnabled; }
}
