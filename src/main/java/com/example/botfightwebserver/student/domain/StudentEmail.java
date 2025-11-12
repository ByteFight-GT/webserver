package com.example.botfightwebserver.student.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_email")
@Getter
public class StudentEmail {
    @Id
    @Column(nullable = false, unique = true)
    private String email;
}
