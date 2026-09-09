package com.nivya.core.network.dto

import com.google.gson.annotations.SerializedName

data class AlertDto(
    @SerializedName("id") val id: Long,
    @SerializedName("familyId") val familyId: Long? = null,
    @SerializedName("deviceId") val deviceId: Long? = null,
    @SerializedName("deviceName") val deviceName: String? = null,
    @SerializedName("deviceUuid") val deviceUuid: String? = null,
    @SerializedName("alertType") val alertType: String,
    @SerializedName("severity") val severity: String = "WARNING",
    @SerializedName("title") val title: String,
    @SerializedName("message") val message: String,
    @SerializedName("resolved") val resolved: Boolean = false,
    @SerializedName("resolvedAt") val resolvedAt: String? = null,
    @SerializedName("isRead") val isRead: Boolean = false,
    @SerializedName("readAt") val readAt: String? = null,
    @SerializedName("targetRole") val targetRole: String = "PARENT",
    @SerializedName("createdAt") val createdAt: String
)

data class AlertRuleDto(
    @SerializedName("id") val id: Long,
    @SerializedName("familyId") val familyId: Long? = null,
    @SerializedName("ruleType") val ruleType: String,
    @SerializedName("thresholdValue") val thresholdValue: String,
    @SerializedName("severity") val severity: String = "WARNING",
    @SerializedName("targetRole") val targetRole: String = "PARENT",
    @SerializedName("enabled") val enabled: Boolean = true,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

data class UpdateAlertRuleRequestDto(
    @SerializedName("thresholdValue") val thresholdValue: String? = null,
    @SerializedName("severity") val severity: String? = null,
    @SerializedName("enabled") val enabled: Boolean? = null
)

data class UnreadCountDto(
    @SerializedName("unreadCount") val unreadCount: Long = 0,
    @SerializedName("familyId") val familyId: Long? = null
)

data class TriggerAlertRequestDto(
    @SerializedName("deviceUuid") val deviceUuid: String? = null,
    @SerializedName("alertType") val alertType: String,
    @SerializedName("severity") val severity: String = "WARNING",
    @SerializedName("title") val title: String,
    @SerializedName("message") val message: String,
    @SerializedName("targetRole") val targetRole: String = "PARENT"
)
