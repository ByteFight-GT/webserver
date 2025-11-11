package com.example.botfightwebserver.student.application;

import com.example.botfightwebserver.student.domain.StudentEmail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentEmailRepository extends JpaRepository<StudentEmail, Long> {
    boolean existsByEmail(String email);
}
