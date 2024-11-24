package com.example.botfightwebserver.submission;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SUBMISSION_VALIDITYTest {

    @Test
    void valueOf_ShouldReturnCorrectEnumValues() {
        // Act & Assert
        assertEquals(SUBMISSION_VALIDITY.VALID, SUBMISSION_VALIDITY.valueOf("VALID"), "Should return VALID enum value");
        assertEquals(SUBMISSION_VALIDITY.INVALID, SUBMISSION_VALIDITY.valueOf("INVALID"), "Should return INVALID enum value");
        assertEquals(SUBMISSION_VALIDITY.NOT_EVALUATED, SUBMISSION_VALIDITY.valueOf("NOT_EVALUATED"), "Should return NOT_EVALUATED enum value");
    }

    @Test
    void values_ShouldReturnAllEnumValues() {
        // Act
        SUBMISSION_VALIDITY[] values = SUBMISSION_VALIDITY.values();

        // Assert
        assertEquals(3, values.length, "Should have exactly 3 enum values");
        assertTrue(containsValue(values, SUBMISSION_VALIDITY.VALID), "Should contain VALID");
        assertTrue(containsValue(values, SUBMISSION_VALIDITY.INVALID), "Should contain INVALID");
        assertTrue(containsValue(values, SUBMISSION_VALIDITY.NOT_EVALUATED), "Should contain NOT_EVALUATED");
    }

    @Test
    void valueOf_ShouldThrowExceptionForInvalidValue() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> SUBMISSION_VALIDITY.valueOf("INVALID_ENUM"),
                "Should throw IllegalArgumentException for invalid enum value"
        );
    }

    @Test
    void name_ShouldReturnCorrectStringRepresentation() {
        // Act & Assert
        assertEquals("VALID", SUBMISSION_VALIDITY.VALID.name(), "VALID name should match");
        assertEquals("INVALID", SUBMISSION_VALIDITY.INVALID.name(), "INVALID name should match");
        assertEquals("NOT_EVALUATED", SUBMISSION_VALIDITY.NOT_EVALUATED.name(), "NOT_EVALUATED name should match");
    }

    @Test
    void toString_ShouldReturnSameAsName() {
        // Act & Assert
        assertEquals(SUBMISSION_VALIDITY.VALID.name(), SUBMISSION_VALIDITY.VALID.toString(), "toString should match name for VALID");
        assertEquals(SUBMISSION_VALIDITY.INVALID.name(), SUBMISSION_VALIDITY.INVALID.toString(), "toString should match name for INVALID");
        assertEquals(SUBMISSION_VALIDITY.NOT_EVALUATED.name(), SUBMISSION_VALIDITY.NOT_EVALUATED.toString(), "toString should match name for NOT_EVALUATED");
    }

    @Test
    void ordinal_ShouldReturnCorrectOrder() {
        // Assert
        assertTrue(SUBMISSION_VALIDITY.VALID.ordinal() != SUBMISSION_VALIDITY.INVALID.ordinal(),
                "VALID and INVALID should have different ordinal values");
    }

    private boolean containsValue(SUBMISSION_VALIDITY[] values, SUBMISSION_VALIDITY value) {
        for (SUBMISSION_VALIDITY v : values) {
            if (v == value) {
                return true;
            }
        }
        return false;
    }
}