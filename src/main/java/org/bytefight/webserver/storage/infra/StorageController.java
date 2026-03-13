package org.bytefight.webserver.storage.infra;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.bytefight.webserver.storage.application.HmacService;
import org.bytefight.webserver.storage.application.LocalStorageService;
import org.bytefight.webserver.storage.application.LocalStorageService.DecompressionLimitExceededException;
import org.bytefight.webserver.storage.domain.StoredObject;
import org.springframework.core.io.Resource;
import org.springframework.core.io.InputStreamResource;
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
      LocalStorageService.DownloadStream download = storageService.openDownloadStream(uuid);
      Resource res = new InputStreamResource(download.stream());
      String etag =
          download.decompressed()
              ? "W/\"" + download.sha256() + "\""
              : "\"" + download.sha256() + "\"";
      return ResponseEntity.ok()
          .contentType(MediaType.parseMediaType(download.contentType()))
          .eTag(etag)
          .header(
              HttpHeaders.CONTENT_DISPOSITION,
              ContentDisposition.attachment()
                  .filename(download.filename(), StandardCharsets.UTF_8)
                  .build()
                  .toString())
          .body(res);
    } catch (DecompressionLimitExceededException e) {
      return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
    } catch (FileNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    } catch (IOException e) {
      return ResponseEntity.internalServerError().build();
    }
  }
}
