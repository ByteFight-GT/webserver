package org.bytefight.webserver.storage.infra;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.bytefight.webserver.storage.application.HmacService;
import org.bytefight.webserver.storage.application.LocalStorageService;
import org.bytefight.webserver.storage.domain.StoredObject;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "File Service")
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class StorageController {
  private final LocalStorageService storageService;
  private final HmacService hmacService;

  @GetMapping("/{uuid}")
  public ResponseEntity<Resource> download(
      @PathVariable String uuid, @RequestParam String exp, @RequestParam String sig) {
    StoredObject storedObject =
        storageService
            .stat(uuid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!hmacService.verify(storedObject, exp, sig)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    try {
      Resource res = storageService.loadAsResource(uuid);
      return ResponseEntity.ok()
          .contentType(MediaType.parseMediaType(storedObject.getContentType()))
          .eTag("\"" + storedObject.getSha256() + "\"")
          .header(
              HttpHeaders.CONTENT_DISPOSITION,
              ContentDisposition.attachment()
                  .filename(storedObject.getFilename(), StandardCharsets.UTF_8)
                  .build()
                  .toString())
          .body(res);
    } catch (IOException e) {
      return ResponseEntity.internalServerError().build();
    }
  }
}
