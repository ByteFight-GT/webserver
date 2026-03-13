package org.bytefight.webserver.storage.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bytefight.webserver.storage.domain.DownloadLinkDto;
import org.bytefight.webserver.storage.domain.FileRecord;
import org.bytefight.webserver.storage.domain.StoredObject;
import org.bytefight.webserver.storage.infra.FileRecordRepository;
import org.bytefight.webserver.storage.infra.StorageProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import com.github.luben.zstd.ZstdInputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocalStorageService {
  private final FileRecordRepository fileRecordRepository;
  private final StorageProperties props;
  private final HmacService hmacService;

  private static final Pattern SAFE = Pattern.compile("[^A-Za-z0-9._-]");

  private static String sanitize(String name) {
    String cleaned = SAFE.matcher(name).replaceAll("_");
    return cleaned.length() > 180 ? cleaned.substring(0, 180) : cleaned;
  }

  public static String sanitizePath(String path) {
    return Arrays.stream(path.split("/"))
        .map(LocalStorageService::sanitize)
        .collect(Collectors.joining("/"));
  }

  private static String extension(String name) {
    int i = name.lastIndexOf('.');
    return (i > 0 && i < name.length() - 1) ? name.substring(i + 1) : "";
  }

  private static String bytesToHex(byte[] bytes) {
    var sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) sb.append(String.format("%02x", b));
    return sb.toString();
  }

  public FileRecord store(MultipartFile file, String logicalPath, String desiredName)
      throws IOException {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("File is null or empty");
    }

    if (logicalPath == null || logicalPath.isEmpty()) {
      throw new IllegalArgumentException("File path is missing");
    }

    String safeName = sanitize(desiredName != null ? desiredName : file.getOriginalFilename());
    String ext = extension(safeName);
    UUID objectId = UUID.randomUUID();

    Path dir = props.root().resolve(sanitizePath(logicalPath)).normalize();
    Files.createDirectories(dir);

    Path target = dir.resolve(objectId + (ext.isEmpty() ? "" : ("." + ext))).normalize();
    Path tmpDir = props.root().resolve(".tmp").normalize();
    Files.createDirectories(tmpDir);
    Path tempTarget =
        tmpDir.resolve(objectId + (ext.isEmpty() ? "" : ("." + ext)) + ".tmp").normalize();

    MessageDigest digest = null;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
    try (InputStream in = file.getInputStream();
        DigestInputStream dis = new DigestInputStream(in, digest);
        OutputStream out = Files.newOutputStream(tempTarget, StandardOpenOption.CREATE_NEW); ) {
      dis.transferTo(out);
    } catch (IOException | RuntimeException ex) {
      deleteQuietly(tempTarget);
      throw ex;
    }

    byte[] hash = digest.digest();
    long size = Files.size(tempTarget);

    String sha256 = bytesToHex(hash);
    String ctype = Optional.ofNullable(file.getContentType()).orElse("application/octet-stream");

    FileRecord rec = new FileRecord();
    rec.setUuid(objectId);
    rec.setFilename(safeName);
    rec.setContentType(ctype);
    rec.setSize(size);
    rec.setSha256(sha256);
    rec.setStoragePath(target.toString());

    try {
      rec = fileRecordRepository.save(rec);
    } catch (RuntimeException ex) {
      deleteQuietly(tempTarget);
      throw ex;
    }

    FileRecord savedRecord = rec;
    if (TransactionSynchronizationManager.isActualTransactionActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              finalizeFile(tempTarget, target, savedRecord);
            }

            @Override
            public void afterCompletion(int status) {
              if (status != TransactionSynchronization.STATUS_COMMITTED) {
                deleteQuietly(tempTarget);
              }
            }
          });
    } else {
      finalizeFile(tempTarget, target, savedRecord);
    }

    return savedRecord;
  }

  private void finalizeFile(Path tempTarget, Path target, FileRecord record) {
    try {
      Files.move(tempTarget, target, StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException | RuntimeException ex) {
      deleteQuietly(tempTarget);
      deleteRecord(record);
      throw new IllegalStateException("Failed to finalize stored file", ex);
    }
  }

  private void deleteRecord(FileRecord record) {
    try {
      fileRecordRepository.delete(record);
    } catch (RuntimeException ignored) {
    }
  }

  private void deleteQuietly(Path path) {
    try {
      Files.deleteIfExists(path);
    } catch (IOException ignored) {
    }
  }

  public Optional<StoredObject> stat(String uuid) {
    return fileRecordRepository
        .findByUuidAndIsDeletedFalse(UUID.fromString(uuid))
        .map(StoredObject::from);
  }

  public Resource loadAsResource(String uuid) throws IOException {
    FileRecord rec =
        fileRecordRepository
            .findByUuidAndIsDeletedFalse(UUID.fromString(uuid))
            .orElseThrow(FileNotFoundException::new);
    Path p = Path.of(rec.getStoragePath());
    if (!Files.exists(p)) throw new FileNotFoundException(uuid);
    return new UrlResource(p.toUri());
  }

  public DownloadStream openDownloadStream(String uuid) throws IOException {
    FileRecord rec =
        fileRecordRepository
            .findByUuidAndIsDeletedFalse(UUID.fromString(uuid))
            .orElseThrow(FileNotFoundException::new);

    Path path = Path.of(rec.getStoragePath());
    if (!Files.exists(path)) throw new FileNotFoundException(uuid);

    InputStream stream = Files.newInputStream(path, StandardOpenOption.READ);

    boolean isZstd = isZstdCodec(rec.getCompressionCodec());
    if (isZstd) {
      stream = new ZstdInputStream(stream);
      stream = applyDecompressionLimit(stream);
    }

    String filename = resolveDownloadFilename(rec.getFilename(), isZstd);
    String contentType = resolveDownloadContentType(rec.getContentType(), filename, isZstd);

    return new DownloadStream(stream, filename, contentType, rec.getSha256(), isZstd);
  }

  private InputStream applyDecompressionLimit(InputStream stream) {
    Long maxBytes = props.maxDecompressedBytes();
    if (maxBytes == null || maxBytes <= 0) {
      return stream;
    }

    return new FilterInputStream(stream) {
      private long seen = 0L;

      @Override
      public int read() throws IOException {
        int value = super.read();
        if (value >= 0) {
          increment(1);
        }
        return value;
      }

      @Override
      public int read(byte[] b, int off, int len) throws IOException {
        int count = super.read(b, off, len);
        if (count > 0) {
          increment(count);
        }
        return count;
      }

      private void increment(int count) throws DecompressionLimitExceededException {
        seen += count;
        if (seen > maxBytes) {
          throw new DecompressionLimitExceededException(
              "Decompressed size exceeds limit: " + maxBytes);
        }
      }
    };
  }

  private static boolean isZstdCodec(String codec) {
    return codec != null && codec.equalsIgnoreCase("zstd");
  }

  private static String resolveDownloadFilename(String filename, boolean isZstd) {
    if (!isZstd || filename == null) {
      return filename;
    }
    return stripZstdExtension(filename);
  }

  private static String stripZstdExtension(String filename) {
    String lower = filename.toLowerCase(Locale.ROOT);
    if (lower.endsWith(".zst")) {
      return filename.substring(0, filename.length() - 4);
    }
    if (lower.endsWith(".zstd")) {
      return filename.substring(0, filename.length() - 5);
    }
    return filename;
  }

  private static String resolveDownloadContentType(
      String storedContentType, String filename, boolean isZstd) {
    if (!isZstd) {
      return storedContentType;
    }

    String guessed = filename != null ? URLConnection.guessContentTypeFromName(filename) : null;
    if (guessed != null) {
      return guessed;
    }
    if (storedContentType != null && !isZstdContentType(storedContentType)) {
      return storedContentType;
    }
    return "application/octet-stream";
  }

  private static boolean isZstdContentType(String contentType) {
    String normalized = contentType.toLowerCase(Locale.ROOT);
    return normalized.equals("application/zstd") || normalized.equals("application/x-zstd");
  }

  public DownloadLinkDto getDownloadLink(String uuid, Duration ttl) {
    StoredObject object = stat(uuid).orElseThrow();

    URI base = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUri();

    long exp = Instant.now().plus(ttl).getEpochSecond();
    String sig = hmacService.sign(object, exp);

    return new DownloadLinkDto(
        UriComponentsBuilder.fromUri(base)
            .path("/files/" + object.getUuid().toString())
            .queryParam("exp", exp)
            .queryParam("sig", sig)
            .build(true)
            .toUri(),
        exp);
  }

  public boolean delete(String uuid) {
    Optional<FileRecord> existing = fileRecordRepository.findByUuid(UUID.fromString(uuid));
    if (existing.isEmpty()) {
      return false;
    }

    FileRecord record = existing.get();
    boolean alreadyDeleted = record.isDeleted();
    if (!alreadyDeleted) {
      record.softDelete();
      fileRecordRepository.save(record);
    }

    Path root = props.root().toAbsolutePath().normalize();
    Path filePath = Path.of(record.getStoragePath()).toAbsolutePath().normalize();
    if (!filePath.startsWith(root)) {
      log.warn(
          "Refusing to delete storage path outside root: uuid={} path={}",
          record.getUuid(),
          filePath);
      return true;
    }

    if (TransactionSynchronizationManager.isActualTransactionActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              deletePhysicalFile(filePath, record.getUuid());
            }
          });
    } else {
      deletePhysicalFile(filePath, record.getUuid());
    }

    return true;
  }

  private void deletePhysicalFile(Path path, UUID uuid) {
    try {
      Files.deleteIfExists(path);
    } catch (IOException ex) {
      log.warn("Failed to delete storage file: uuid={} path={}", uuid, path, ex);
    }
  }

  public record DownloadStream(
      InputStream stream, String filename, String contentType, String sha256, boolean decompressed) {}

  public static class DecompressionLimitExceededException extends IOException {
    public DecompressionLimitExceededException(String message) {
      super(message);
    }
  }
}
