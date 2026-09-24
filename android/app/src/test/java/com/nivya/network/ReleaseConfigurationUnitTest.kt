package com.nivya.network

import com.nivya.core.network.NetworkConfig
import com.nivya.services.notification.ConvocationNotificationManager
import org.junit.Assert.*
import org.junit.Test

/**
 * Phase 7 Production Release Configuration & Network Invariants Unit Tests.
 */
class ReleaseConfigurationUnitTest {

    @Test
    fun testProductionApiUrl_ExactMatch() {
        assertEquals("https://nivya-blbf.onrender.com/api/v1/", NetworkConfig.PROD_API_BASE_URL)
    }

    @Test
    fun testProductionWebSocketUrl_ExactMatch() {
        assertEquals("wss://nivya-blbf.onrender.com/ws/websocket", NetworkConfig.PROD_WS_URL)
    }

    @Test
    fun testRetrofitPathConcatenationSafety_Production() {
        // When configured with production base URL, Retrofit base must normalize to the host root with slash
        // so that NivyaApiService endpoints declaring "api/v1/..." resolve to "https://nivya-blbf.onrender.com/api/v1/..."
        val retrofitBase = NetworkConfig.getRetrofitBaseUrl(NetworkConfig.PROD_API_BASE_URL)
        assertEquals("https://nivya-blbf.onrender.com/", retrofitBase)

        // Verify simulated concatenation with an API endpoint
        val sampleEndpoint = "api/v1/auth/login"
        val fullUrl = retrofitBase + sampleEndpoint
        assertEquals("https://nivya-blbf.onrender.com/api/v1/auth/login", fullUrl)
        assertFalse("Url must not contain duplicate api/v1 paths", fullUrl.contains("api/v1/api/v1"))
    }

    @Test
    fun testRetrofitPathConcatenationSafety_Debug() {
        val devBase = "http://10.0.2.2:8080/"
        val retrofitBase = NetworkConfig.getRetrofitBaseUrl(devBase)
        assertEquals("http://10.0.2.2:8080/", retrofitBase)

        val sampleEndpoint = "api/v1/auth/register"
        val fullUrl = retrofitBase + sampleEndpoint
        assertEquals("http://10.0.2.2:8080/api/v1/auth/register", fullUrl)
    }

    @Test
    fun testTokenRefreshUrlResolution_ProductionAndDev() {
        val prodRefresh = NetworkConfig.getRefreshUrl(NetworkConfig.PROD_API_BASE_URL)
        assertEquals("https://nivya-blbf.onrender.com/api/v1/auth/refresh", prodRefresh)
        assertFalse("Refresh URL must not have duplicate /api/v1", prodRefresh.contains("api/v1/api/v1"))

        val devRefresh = NetworkConfig.getRefreshUrl("http://10.0.2.2:8080/")
        assertEquals("http://10.0.2.2:8080/api/v1/auth/refresh", devRefresh)
    }

    @Test
    fun testWebSocketUrlDerivation_Production() {
        val wsUrl = NetworkConfig.getWebSocketUrl(NetworkConfig.PROD_API_BASE_URL)
        assertEquals("wss://nivya-blbf.onrender.com/ws/websocket", wsUrl)
        assertTrue("Production WebSocket must use WSS scheme", wsUrl.startsWith("wss://"))
        assertFalse("Production WebSocket must not contain 10.0.2.2", wsUrl.contains("10.0.2.2"))
        assertFalse("Production WebSocket must not contain localhost", wsUrl.contains("localhost"))
    }

    @Test
    fun testWebSocketUrlDerivation_Dev() {
        val wsUrl = NetworkConfig.getWebSocketUrl("http://10.0.2.2:8080/")
        assertEquals("ws://10.0.2.2:8080/ws/websocket", wsUrl)
    }

    @Test
    fun testEndpointSecurity_NoCleartextInProduction() {
        assertTrue(NetworkConfig.isSecure(NetworkConfig.PROD_API_BASE_URL))
        assertTrue(NetworkConfig.isSecure(NetworkConfig.PROD_WS_URL))

        assertFalse(NetworkConfig.isSecure("http://10.0.2.2:8080/"))
        assertFalse(NetworkConfig.isSecure("ws://10.0.2.2:8080/ws/websocket"))
        assertFalse(NetworkConfig.isSecure("http://nivya-blbf.onrender.com/api/v1/"))
    }

    @Test
    fun testFcmConvocationDecoyNotification_InvariantsPreserved() {
        assertEquals("Check your battery status", ConvocationNotificationManager.DECOY_MESSAGE)
        assertEquals("Device Update", ConvocationNotificationManager.DECOY_TITLE)
        assertEquals("nivya_device_status_sync", ConvocationNotificationManager.CHANNEL_ID)
    }

    @Test
    fun testNoHardcodedSecretsInNetworkConfig() {
        assertFalse(NetworkConfig.PROD_API_BASE_URL.contains("password"))
        assertFalse(NetworkConfig.PROD_API_BASE_URL.contains("secret"))
        assertFalse(NetworkConfig.PROD_WS_URL.contains("password"))
        assertFalse(NetworkConfig.PROD_WS_URL.contains("secret"))
    }
}
