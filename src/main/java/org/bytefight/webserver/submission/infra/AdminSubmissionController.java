package org.bytefight.webserver.submission.infra;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.io.IOException;
import java.util.Set;

import org.bytefight.webserver.common.web.RestPageRequest;
import org.bytefight.webserver.submission.application.AdminSubmissionService;
import org.bytefight.webserver.submission.domain.Submission;
import org.bytefight.webserver.submission.domain.dto.AdminCreateSubmissionDto;
import org.bytefight.webserver.submission.domain.dto.AdminSubmissionDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Submissions (Admin)")
@RequestMapping("/api/v1/admin/submission")
@PreAuthorize("hasRole('ADMIN')")
@RestController
public class AdminSubmissionController {
  private static final int DEFAULT_PAGE_SIZE = 25;
  private static final int MAX_PAGE_SIZE = 100;
  private static final String DEFAULT_SORT_FIELD = "createdAt";
  private static final Set<String> ALLOWED_SORT_FIELDS =
      Set.of("createdAt", "id", "uuid", "validity");

  private final AdminSubmissionService adminSubmissionService;

  public AdminSubmissionController(AdminSubmissionService adminSubmissionService) {
    this.adminSubmissionService = adminSubmissionService;
  }

  @GetMapping
  @Operation(
      operationId = "adminListSubmissions",
      summary = "REST endpoint to list all submissions")
  public Page<AdminSubmissionDto> listAll(@ModelAttribute RestPageRequest pageRequest) {
    Pageable pageable =
        pageRequest.toPageable(
            DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE, DEFAULT_SORT_FIELD, ALLOWED_SORT_FIELDS);

    Page<Submission> submissions = adminSubmissionService.listSubmissions(false, pageable);
    var data = submissions.stream().map(AdminSubmissionDto::from).toList();
    return new PageImpl<>(data, submissions.getPageable(), submissions.getTotalElements());
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(
      operationId = "adminCreateSubmission",
      summary = "REST endpoint to create a submission")
  public ResponseEntity<AdminSubmissionDto> createSubmission(
      @Valid @ModelAttribute AdminCreateSubmissionDto input) throws IOException {
    Submission submission = adminSubmissionService.createSubmission(input);
    return ResponseEntity.status(HttpStatus.CREATED).body(AdminSubmissionDto.from(submission));
  }
}
