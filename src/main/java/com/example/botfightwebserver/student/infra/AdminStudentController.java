package com.example.botfightwebserver.student.infra;

import com.example.botfightwebserver.gameMatch.domain.GameMatchDTO;
import com.example.botfightwebserver.student.application.StudentService;
import com.example.botfightwebserver.student.domain.StudentDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/student")
public class AdminStudentController {
    private final StudentService studentService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<StudentDto> adminListAllStudents() {
        List<StudentDto> students = studentService.list();

        return new PageImpl<>(students);
    }
}
