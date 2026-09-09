package com.nivya.alerts

import com.nivya.core.network.dto.AlertDto
import com.nivya.core.network.dto.AlertRuleDto
import com.nivya.services.notification.AlertNotificationManager
import com.nivya.ui.alerts.AlertFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertUnitTest {

    @Test
    fun testAlertDtoDefaultValuesAndProperties() {
        val alert = AlertDto(
            id = 42L,
            familyId = 1L,
            deviceId = 10L,
            deviceName = "Child Galaxy A54",
            deviceUuid = "uuid-child-10",
            alertType = "LOW_BATTERY",
            severity = "WARNING",
            title = "Low Battery Warning",
            message = "Battery reached 14%",
            resolved = false,
            isRead = false,
            targetRole = "ALL",
            createdAt = "2026-09-09T10:00:00"
        )

        assertEquals(42L, alert.id)
        assertEquals("LOW_BATTERY", alert.alertType)
        assertEquals("WARNING", alert.severity)
        assertEquals("ALL", alert.targetRole)
        assertFalse(alert.resolved)
        assertFalse(alert.isRead)
    }

    @Test
    fun testChildAlertSegregationGuardrail() {
        val parentOnlyAlert = AlertDto(
            id = 1L,
            alertType = "SECURITY_ALERT",
            severity = "CRITICAL",
            title = "Security Alert",
            message = "Unauthorized pairing attempt",
            targetRole = "PARENT",
            createdAt = "2026-09-09T10:00:00"
        )

        val childIntendedAlert = AlertDto(
            id = 2L,
            alertType = "LOW_BATTERY",
            severity = "WARNING",
            title = "Plug In Charger",
            message = "Battery reached 15%",
            targetRole = "CHILD",
            createdAt = "2026-09-09T10:05:00"
        )

        val allAlert = AlertDto(
            id = 3L,
            alertType = "DEVICE_ONLINE",
            severity = "INFO",
            title = "Reconnected",
            message = "Device back online",
            targetRole = "ALL",
            createdAt = "2026-09-09T10:10:00"
        )

        val fullAlerts = listOf(parentOnlyAlert, childIntendedAlert, allAlert)

        // Filter simulating Child alert visibility rule: targetRole IN ('CHILD', 'ALL')
        val childVisibleAlerts = fullAlerts.filter {
            "CHILD".equals(it.targetRole, ignoreCase = true) || "ALL".equals(it.targetRole, ignoreCase = true)
        }

        assertEquals(2, childVisibleAlerts.size)
        assertTrue(childVisibleAlerts.none { "PARENT".equals(it.targetRole, ignoreCase = true) })
        assertTrue(childVisibleAlerts.any { it.id == 2L })
        assertTrue(childVisibleAlerts.any { it.id == 3L })
    }

    @Test
    fun testParentFilterLogic() {
        val criticalAlert = AlertDto(
            id = 1L, alertType = "OFFLINE", severity = "CRITICAL",
            title = "Device Offline", message = "Offline > 30m",
            isRead = false, createdAt = "2026-09-09T10:00:00"
        )
        val readAlert = AlertDto(
            id = 2L, alertType = "LOW_BATTERY", severity = "WARNING",
            title = "Battery", message = "12%",
            isRead = true, createdAt = "2026-09-09T10:00:00"
        )
        val securityAlert = AlertDto(
            id = 3L, alertType = "SECURITY_ALERT", severity = "CRITICAL",
            title = "Security", message = "Brute force detected",
            isRead = false, createdAt = "2026-09-09T10:00:00"
        )

        val list = listOf(criticalAlert, readAlert, securityAlert)

        // 1. Unread Filter
        val unread = list.filter { !it.isRead }
        assertEquals(2, unread.size)

        // 2. Critical Filter
        val critical = list.filter { "CRITICAL".equals(it.severity, ignoreCase = true) }
        assertEquals(2, critical.size)

        // 3. Security Filter
        val security = list.filter { "SECURITY_ALERT".equals(it.alertType, ignoreCase = true) }
        assertEquals(1, security.size)
        assertEquals(3L, security.first().id)
    }

    @Test
    fun testAlertRuleDtoModel() {
        val rule = AlertRuleDto(
            id = 1L,
            familyId = 5L,
            ruleType = "LOW_BATTERY",
            thresholdValue = "15",
            severity = "WARNING",
            targetRole = "ALL",
            enabled = true,
            updatedAt = "2026-09-09T10:00:00"
        )

        assertEquals("LOW_BATTERY", rule.ruleType)
        assertEquals("15", rule.thresholdValue)
        assertTrue(rule.enabled)
        assertEquals("ALL", rule.targetRole)
    }

    @Test
    fun testNotificationChannelsArchitecture() {
        assertEquals("nivya_safety_critical", AlertNotificationManager.CHANNEL_SAFETY_CRITICAL)
        assertEquals("nivya_general_alerts", AlertNotificationManager.CHANNEL_GENERAL_ALERTS)
    }
}
