package com.example.botfightwebserver.storage;

import com.example.botfightwebserver.FullStackIntegrationTestBase;
import com.example.botfightwebserver.storage.application.LocalStorageService;
import com.example.botfightwebserver.storage.domain.StoredObject;
import com.example.botfightwebserver.storage.infra.FileRecordRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class LocalStorageServiceIT extends FullStackIntegrationTestBase {
    private static Path storageRoot;

    @Autowired
    private LocalStorageService localStorageService;

    @Autowired
    private FileRecordRepository fileRecordRepository;

    @BeforeAll
    static void setupStorageRoot() throws IOException {
        storageRoot = Files.createTempDirectory("storage-test-");
    }

    @BeforeEach
    void clearStorage() throws IOException {
        fileRecordRepository.deleteAll();
        if (Files.exists(storageRoot)) {
            try (Stream<Path> paths = Files.walk(storageRoot)) {
                paths.sorted(Comparator.reverseOrder())
                        .filter(path -> !path.equals(storageRoot))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException ignored) {
                            }
                        });
            }
        }
    }

    @DynamicPropertySource
    static void configureStorageProperties(DynamicPropertyRegistry registry) {
        registry.add("storage.root", () -> storageRoot.toString());
        registry.add("storage.hmac-secret", () -> "test-secret");
    }

    @Test
    void storeWritesFileAndPersistsRecord() throws Exception {
        byte[] content = "hello storage".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "example.txt",
                "text/plain",
                content
        );

        StoredObject stored = localStorageService.store(file, "submissions/test/", "example.txt");

        assertThat(stored).isNotNull();

        var records = fileRecordRepository.findAll();
        assertThat(records).hasSize(1);
        var record = records.get(0);
        String filename = getField(record, "filename");
        String contentType = getField(record, "contentType");
        Long size = getField(record, "size");
        String storagePath = getField(record, "storagePath");
        assertThat(filename).isEqualTo("example.txt");
        assertThat(contentType).isEqualTo("text/plain");
        assertThat(size).isEqualTo((long) content.length);
        assertThat(storagePath).contains(storageRoot.toString());

        Path storedPath = Path.of(storagePath);
        assertThat(Files.exists(storedPath)).isTrue();
        assertThat(Files.readAllBytes(storedPath)).isEqualTo(content);
    }

    @Test
    void storeRejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                localStorageService.store(file, "submissions/test/", "empty.txt")
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void storeRejectsMissingPath() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "example.txt",
                "text/plain",
                "hello".getBytes()
        );

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                localStorageService.store(file, "", "example.txt")
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void storeSanitizesPathAndFilename() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "bad name.txt",
                "text/plain",
                "hello".getBytes()
        );

        localStorageService.store(file, "submissions/bad path/", "bad name.txt");

        var records = fileRecordRepository.findAll();
        assertThat(records).hasSize(1);
        var record = records.get(0);
        String filename = getField(record, "filename");
        String storagePath = getField(record, "storagePath");
        assertThat(filename).doesNotContain(" ");
        assertThat(storagePath).doesNotContain(" ");
    }

    @Test
    void storeAllowsDuplicateFilenames() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "same.txt",
                "text/plain",
                "hello".getBytes()
        );

        localStorageService.store(file, "submissions/test/", "same.txt");
        localStorageService.store(file, "submissions/test/", "same.txt");

        var records = fileRecordRepository.findAll();
        assertThat(records).hasSize(2);
        String firstPath = getField(records.get(0), "storagePath");
        String secondPath = getField(records.get(1), "storagePath");
        assertThat(firstPath).isNotEqualTo(secondPath);
    }

    @Test
    void storePersistsSha256Integrity() throws Exception {
        byte[] content = "hash me".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hash.txt",
                "text/plain",
                content
        );

        localStorageService.store(file, "submissions/test/", "hash.txt");

        var record = fileRecordRepository.findAll().get(0);
        String sha256 = getField(record, "sha256");
        UUID uuid = getField(record, "uuid");
        assertThat(sha256).isEqualTo(sha256Hex(content));
        assertThat(fileRecordRepository.findByUuid(uuid)).isPresent();
    }

    private static String sha256Hex(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T getField(Object target, String name) {
        return (T) ReflectionTestUtils.getField(target, name);
    }
}
