package com.example.botfightwebserver.player;

import com.example.botfightwebserver.submission.Submission;
import com.google.common.annotations.VisibleForTesting;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;
    @Email
    private String email;

    private LocalDateTime creationDateTime;

    private LocalDateTime lastModifiedDate;
    @Builder.Default
    private Double elo=1200.0;
    @Builder.Default
    private Integer matchesPlayed=0;
    @Builder.Default
    private Integer numberWins=0;
    @Builder.Default
    private Integer numberLosses=0;
    @Builder.Default
    private Integer numberDraws=0;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name="current_submission_id", nullable = true)
    private Submission currentSubmission;

    private static Clock clock = Clock.systemDefaultZone();

    @PrePersist
    public void onCreate() {
        creationDateTime = LocalDateTime.now(clock);
        lastModifiedDate = LocalDateTime.now(clock);
    }

    @PreUpdate
    public void onUpdate() {
        lastModifiedDate = LocalDateTime.now(clock);
    }

    @VisibleForTesting
    public static void setClock(Clock testClock) {
        clock = testClock;
    }
}
