package com.example.botfightwebserver.submission;

import com.example.botfightwebserver.submission.domain.SubmissionValidity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SUBMISSION_VALIDITYTest {

    @Test
    void valueOf_ShouldReturnCorrectEnumValues() {
        // Act & Assert
        assertEquals(SubmissionValidity.VALID, SubmissionValidity.valueOf("VALID"), "Should return VALID enum value");
        assertEquals(SubmissionValidity.INVALID, SubmissionValidity.valueOf("INVALID"), "Should return INVALID enum value");
        assertEquals(SubmissionValidity.NOT_EVALUATED, SubmissionValidity.valueOf("NOT_EVALUATED"), "Should return NOT_EVALUATED enum value");
    }

    @Test
    void values_ShouldReturnAllEnumValues() {
        // Act
        SubmissionValidity[] values = SubmissionValidity.values();

        // Assert
        assertEquals(3, values.length, "Should have exactly 3 enum values");
        assertTrue(containsValue(values, SubmissionValidity.VALID), "Should contain VALID");
        assertTrue(containsValue(values, SubmissionValidity.INVALID), "Should contain INVALID");
        assertTrue(containsValue(values, SubmissionValidity.NOT_EVALUATED), "Should contain NOT_EVALUATED");
    }

    @Test
    void valueOf_ShouldThrowExceptionForInvalidValue() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> SubmissionValidity.valueOf("INVALID_ENUM"),
                "Should throw IllegalArgumentException for invalid enum value"
        );
    }

    @Test
    void name_ShouldReturnCorrectStringRepresentation() {
        // Act & Assert
        assertEquals("VALID", SubmissionValidity.VALID.name(), "VALID name should match");
        assertEquals("INVALID", SubmissionValidity.INVALID.name(), "INVALID name should match");
        assertEquals("NOT_EVALUATED", SubmissionValidity.NOT_EVALUATED.name(), "NOT_EVALUATED name should match");
    }

    @Test
    void toString_ShouldReturnSameAsName() {
        // Act & Assert
        assertEquals(SubmissionValidity.VALID.name(), SubmissionValidity.VALID.toString(), "toString should match name for VALID");
        assertEquals(SubmissionValidity.INVALID.name(), SubmissionValidity.INVALID.toString(), "toString should match name for INVALID");
        assertEquals(SubmissionValidity.NOT_EVALUATED.name(), SubmissionValidity.NOT_EVALUATED.toString(), "toString should match name for NOT_EVALUATED");
    }

    @Test
    void ordinal_ShouldReturnCorrectOrder() {
        // Assert
        assertTrue(SubmissionValidity.VALID.ordinal() != SubmissionValidity.INVALID.ordinal(),
                "VALID and INVALID should have different ordinal values");
    }

    private boolean containsValue(SubmissionValidity[] values, SubmissionValidity value) {
        for (SubmissionValidity v : values) {
            if (v == value) {
                return true;
            }
        }
        return false;
    }
}