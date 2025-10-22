package com.example.botfightwebserver.storage.infra;

import com.example.botfightwebserver.storage.application.HmacService;
import com.example.botfightwebserver.storage.application.LocalStorageService;
import com.example.botfightwebserver.storage.domain.StoredObject;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class StorageController {
    private final LocalStorageService storageService;
    private final HmacService hmacService;

    @GetMapping("/{uuid}")
    public ResponseEntity<Resource> download(@PathVariable String uuid, @RequestParam String exp, @RequestParam String sig) {
        StoredObject storedObject = storageService.stat(uuid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!hmacService.verify(storedObject, exp, sig)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            Resource res = storageService.loadAsResource(uuid);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(storedObject.getContentType()))
                    .eTag("\"" + storedObject.getSha256() + "\"")
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment().filename(storedObject.getFilename(), StandardCharsets.UTF_8).toString())
                    .body(res);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}