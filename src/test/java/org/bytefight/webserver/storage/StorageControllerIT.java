package org.bytefight.webserver.storage;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

import com.github.luben.zstd.Zstd;
import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.storage.application.HmacService;
import org.bytefight.webserver.storage.application.LocalStorageService;
import org.bytefight.webserver.storage.domain.DownloadLinkDto;
import org.bytefight.webserver.storage.domain.FileRecord;
import org.bytefight.webserver.storage.domain.StoredObject;
import org.bytefight.webserver.storage.infra.FileRecordRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

class StorageControllerIT extends FullStackIntegrationTestBase {
  private static Path storageRoot;

  @Autowired private MockMvc mockMvc;

  @Autowired private LocalStorageService localStorageService;

  @Autowired private HmacService hmacService;

  @Autowired private FileRecordRepository fileRecordRepository;

  @BeforeAll
  static void setupStorageRoot() throws IOException {
    storageRoot = Files.createTempDirectory("storage-controller-test-");
  }

  @DynamicPropertySource
  static void configureStorageProperties(DynamicPropertyRegistry registry) {
    registry.add("storage.root", () -> storageRoot.toString());
    registry.add("storage.hmac-secret", () -> "test-secret");
  }

  @BeforeEach
  void clearStorage() throws IOException {
    fileRecordRepository.deleteAll();
    if (Files.exists(storageRoot)) {
      try (Stream<Path> paths = Files.walk(storageRoot)) {
        paths
            .sorted(Comparator.reverseOrder())
            .filter(path -> !path.equals(storageRoot))
            .forEach(
                path -> {
                  try {
                    Files.deleteIfExists(path);
                  } catch (IOException ignored) {
                  }
                });
      }
    }
  }

  @Test
  void downloadSignedFileReturnsContent() throws Exception {
    byte[] contentBytes = "signed content".getBytes();
    MockMultipartFile file =
        new MockMultipartFile("file", "download.txt", "text/plain", contentBytes);

    localStorageService.store(file, "downloads/test/", "download.txt");

    var record = fileRecordRepository.findAll().get(0);
    UUID uuid = getField(record, "uuid");
    DownloadLinkDto link =
        localStorageService.getDownloadLink(uuid.toString(), Duration.ofMinutes(5));
    java.net.URI uri = getField(link, "uri");

    String requestPath = uri.getPath() + "?" + uri.getQuery();

    mockMvc
        .perform(get(requestPath))
        .andExpect(status().isOk())
        .andExpect(content().contentType("text/plain"))
        .andExpect(content().bytes(contentBytes));
  }

  @Test
  void downloadSignedFileRejectsExpiredSignature() throws Exception {
    byte[] contentBytes = "expired content".getBytes();
    MockMultipartFile file =
        new MockMultipartFile("file", "expired.txt", "text/plain", contentBytes);

    StoredObject stored =
        StoredObject.from(localStorageService.store(file, "downloads/test/", "expired.txt"));
    long exp = Instant.now().minusSeconds(60).getEpochSecond();
    String sig = hmacService.sign(stored, exp);

    UUID uuid = getField(stored, "uuid");
    mockMvc
        .perform(
            get("/files/{uuid}", uuid)
                .queryParam("exp", String.valueOf(exp))
                .queryParam("sig", sig))
        .andExpect(status().isForbidden());
  }

  @Test
  void downloadSignedFileRejectsTamperedSignature() throws Exception {
    byte[] contentBytes = "tampered content".getBytes();
    MockMultipartFile file =
        new MockMultipartFile("file", "tampered.txt", "text/plain", contentBytes);

    localStorageService.store(file, "downloads/test/", "tampered.txt");

    var record = fileRecordRepository.findAll().get(0);
    UUID uuid = getField(record, "uuid");
    DownloadLinkDto link =
        localStorageService.getDownloadLink(uuid.toString(), Duration.ofMinutes(5));
    java.net.URI uri = getField(link, "uri");

    String query = uri.getQuery();
    String tamperedSig = query.replaceFirst("sig=", "sig=invalid");

    String requestPath = uri.getPath() + "?" + tamperedSig;

    mockMvc.perform(get(requestPath)).andExpect(status().isForbidden());
  }

  @Test
  void downloadSignedFileDecompressesZstd() throws Exception {
    byte[] originalBytes = "zstd payload".getBytes();
    byte[] compressedBytes = Zstd.compress(originalBytes);
    MockMultipartFile file =
        new MockMultipartFile("file", "payload.txt.zst", "application/zstd", compressedBytes);

    FileRecord record = localStorageService.store(file, "downloads/test/", "payload.txt.zst");
    record.setCompressionCodec("zstd");
    fileRecordRepository.save(record);

    DownloadLinkDto link =
        localStorageService.getDownloadLink(record.getUuid().toString(), Duration.ofMinutes(5));
    java.net.URI uri = getField(link, "uri");

    String requestPath = uri.getPath() + "?" + uri.getQuery();

    mockMvc
        .perform(get(requestPath))
        .andExpect(status().isOk())
        .andExpect(content().contentType("text/plain"))
        .andExpect(content().bytes(originalBytes));
  }

  @Test
  void downloadSignedFileMissingReturnsNotFound() throws Exception {
    UUID missingUuid = UUID.randomUUID();
    FileRecord record = new FileRecord();
    ReflectionTestUtils.setField(record, "uuid", missingUuid);
    ReflectionTestUtils.setField(record, "filename", "missing.txt");
    ReflectionTestUtils.setField(record, "contentType", "text/plain");
    ReflectionTestUtils.setField(record, "size", 1L);
    ReflectionTestUtils.setField(record, "sha256", "deadbeef");
    ReflectionTestUtils.setField(
        record, "storagePath", storageRoot.resolve("missing.txt").toString());
    StoredObject stored = StoredObject.from(record);
    long exp = Instant.now().plusSeconds(300).getEpochSecond();
    String sig = hmacService.sign(stored, exp);

    mockMvc
        .perform(
            get("/files/{uuid}", missingUuid)
                .queryParam("exp", String.valueOf(exp))
                .queryParam("sig", sig))
        .andExpect(status().isNotFound());
  }

  @SuppressWarnings("unchecked")
  private static <T> T getField(Object target, String name) {
    return (T) ReflectionTestUtils.getField(target, name);
  }
}
