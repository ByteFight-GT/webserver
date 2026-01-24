package com.example.botfightwebserver.storage.application;

import com.example.botfightwebserver.storage.domain.DownloadLinkDto;
import com.example.botfightwebserver.storage.domain.FileRecord;
import com.example.botfightwebserver.storage.infra.StorageProperties;
import com.example.botfightwebserver.storage.domain.StoredObject;
import com.example.botfightwebserver.storage.infra.FileRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
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

    public StoredObject store(MultipartFile file, String logicalPath, String desiredName) throws IOException {
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

        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        try (InputStream in = file.getInputStream();
             DigestInputStream dis = new DigestInputStream(in, digest);
             OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW);
        ) {
            dis.transferTo(out);
        }

        byte[] hash = digest.digest();
        long size = Files.size(target);

        String sha256 = bytesToHex(hash);
        String ctype = Optional.ofNullable(file.getContentType()).orElse("application/octet-stream");

//        FileRecord rec = fileRecordRepository.save(new FileRecord(
//                null,
//                objectId,
//                safeName,
//                ctype,
//                size,
//                sha256,
//                target.toString(),
//                Instant.now(),
//                Instant.now()
//        ));
//
//        return StoredObject.from(rec);
        return null;
    }

    public Optional<StoredObject> stat(String uuid) {
        return fileRecordRepository.findByUuid(UUID.fromString(uuid)).map(StoredObject::from);
    }

    public Resource loadAsResource(String uuid) throws IOException {
        FileRecord rec = fileRecordRepository.findByUuid(UUID.fromString(uuid)).orElseThrow(FileNotFoundException::new);
        Path p = Path.of(rec.getStoragePath());
        if (!Files.exists(p)) throw new FileNotFoundException(uuid);
        return new UrlResource(p.toUri());
    }

    public DownloadLinkDto getDownloadLink(String uuid, Duration ttl) {
        StoredObject object = stat(uuid).orElseThrow();

        URI base = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .build()
                .toUri();

        long exp = Instant.now().plus(ttl).getEpochSecond();
        String sig = hmacService.sign(object, exp);

        return new DownloadLinkDto(
                UriComponentsBuilder
                        .fromUri(base)
                        .path("/files/" + object.getUuid().toString())
                        .queryParam("exp", exp)
                        .queryParam("sig", sig)
                        .build(true)
                        .toUri(),
                exp
        );
    }
}