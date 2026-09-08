package com.nivya.common;

import com.nivya.common.response.ApiResponse;
import com.nivya.common.response.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests for Common API Response Models.
 */
class ApiResponseTest {

    @Test
    @DisplayName("ApiResponse.success should format payload correctly")
    void testSuccessResponse() {
        String testData = "Nivya Core Test";
        ApiResponse<String> response = ApiResponse.success(testData, "All Systems Normal");

        assertTrue(response.isSuccess());
        assertEquals("All Systems Normal", response.getMessage());
        assertEquals(testData, response.getData());
        assertNotNull(response.getTimestamp());
        assertNotNull(response.getTraceId());
    }

    @Test
    @DisplayName("ApiResponse.error should format failure response")
    void testErrorResponse() {
        ApiResponse<Void> response = ApiResponse.error("Something went wrong");

        assertFalse(response.isSuccess());
        assertEquals("Something went wrong", response.getMessage());
        assertNull(response.getData());
        assertNotNull(response.getTimestamp());
        assertNotNull(response.getTraceId());
    }

    @Test
    @DisplayName("ErrorResponse should contain structured error details")
    void testStructuredErrorResponse() {
        ErrorResponse error = new ErrorResponse(
                400,
                "Bad Request",
                "Invalid input parameters",
                "/api/v1/test",
                Map.of("field", "Must not be blank")
        );

        assertFalse(error.isSuccess());
        assertEquals(400, error.getStatus());
        assertEquals("Bad Request", error.getError());
        assertEquals("Invalid input parameters", error.getMessage());
        assertEquals("/api/v1/test", error.getPath());
        assertNotNull(error.getTimestamp());
        assertEquals("Must not be blank", error.getValidationErrors().get("field"));
    }
}
