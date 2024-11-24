package com.example.botfightwebserver.submission;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class STORAGE_SOURCETest {

    @Test
    void valueOf_ShouldReturnCorrectEnumValues() {
        // Act & Assert
        assertEquals(STORAGE_SOURCE.GCP, STORAGE_SOURCE.valueOf("GCP"), "Should return GCP enum value");
        assertEquals(STORAGE_SOURCE.LOCAL, STORAGE_SOURCE.valueOf("LOCAL"), "Should return LOCAL enum value");
    }

    @Test
    void values_ShouldReturnAllEnumValues() {
        // Act
        STORAGE_SOURCE[] values = STORAGE_SOURCE.values();

        // Assert
        assertEquals(2, values.length, "Should have exactly 2 enum values");
        assertTrue(containsValue(values, STORAGE_SOURCE.GCP), "Should contain GCP");
        assertTrue(containsValue(values, STORAGE_SOURCE.LOCAL), "Should contain LOCAL");
    }

    @Test
    void valueOf_ShouldThrowExceptionForInvalidValue() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> STORAGE_SOURCE.valueOf("INVALID"),
                "Should throw IllegalArgumentException for invalid enum value"
        );
    }

    @Test
    void name_ShouldReturnCorrectStringRepresentation() {
        // Act & Assert
        assertEquals("GCP", STORAGE_SOURCE.GCP.name(), "GCP name should match");
        assertEquals("LOCAL", STORAGE_SOURCE.LOCAL.name(), "LOCAL name should match");
    }

    @Test
    void toString_ShouldReturnSameAsName() {
        // Act & Assert
        assertEquals(STORAGE_SOURCE.GCP.name(), STORAGE_SOURCE.GCP.toString(), "toString should match name for GCP");
        assertEquals(STORAGE_SOURCE.LOCAL.name(), STORAGE_SOURCE.LOCAL.toString(), "toString should match name for LOCAL");
    }

    @Test
    void ordinal_ShouldReturnCorrectOrder() {
        // Assert
        assertTrue(STORAGE_SOURCE.GCP.ordinal() != STORAGE_SOURCE.LOCAL.ordinal(),
                "GCP and LOCAL should have different ordinal values");
    }

    private boolean containsValue(STORAGE_SOURCE[] values, STORAGE_SOURCE value) {
        for (STORAGE_SOURCE v : values) {
            if (v == value) {
                return true;
            }
        }
        return false;
    }
}