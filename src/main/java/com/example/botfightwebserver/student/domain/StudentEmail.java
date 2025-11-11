package com.example.botfightwebserver.student.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_email")
public class StudentEmail {
    @Id
    @Column(nullable = false, unique = true)
    private String email;
}
