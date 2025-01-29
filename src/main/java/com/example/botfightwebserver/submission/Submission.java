package com.example.botfightwebserver.submission;

import com.google.common.annotations.VisibleForTesting;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table
@Builder
@NoArgsConstructor @AllArgsConstructor @Getter @Setter
public class Submission {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String storagePath;

    private Long teamId;

    @Enumerated(EnumType.STRING)
    private SUBMISSION_VALIDITY submissionValidity;

    @Enumerated(EnumType.STRING)
    private STORAGE_SOURCE source;

    private LocalDateTime createdAt;

    private LocalDateTime validateAt;

    private String name;

    private static Clock clock = Clock.system(ZoneId.of("America/New_York"));

    private Boolean isAutoSet;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(clock);
    }


    public void setSubmissionValidity(SUBMISSION_VALIDITY submissionValidity) {
        this.submissionValidity = submissionValidity;
        if (submissionValidity == SUBMISSION_VALIDITY.VALID) {
            validateAt = LocalDateTime.now(clock);
        }
    }

    @VisibleForTesting
    public static void setClock(Clock testClock) {
        clock = testClock;
    }
}
