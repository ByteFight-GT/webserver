package com.example.botfightwebserver.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.NoResultException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LocalStorageService {
    private Clock clock;
    private final Path storageDir = Paths.get("storage"); // Base storage directory
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String JSON_EXTENSION = ".json";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    public LocalStorageService() {
        this.clock = Clock.systemDefaultZone();
    }

    public String uploadFile(Long teamId, MultipartFile file, String filepath) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is null or empty");
        }
        if (filepath == null || filepath.isEmpty()) {
            throw new IllegalArgumentException("File path is missing");
        }

        // Creates the folder object, then checks that it can create that folder or if it already exists
        File folder = new File(String.valueOf(filepath));
        if (folder.mkdir()){
            System.out.println("success");
        }
        else {
            System.out.println("Folder Exists");
        }

        // Extracting the filename from the file and getting the time of file creation
        String fileName = file.getOriginalFilename() != null && !file.getOriginalFilename().isEmpty() ?
                file.getOriginalFilename() : "unknown";
        String timestamp = LocalDateTime.now(clock).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        // Creates new file object with the filepath of the folder, then adding the name of the file to log into the area
        File dest = new File(filepath + fileName + timestamp);

        if (!dest.isFile()) {
            // We add the new file to the storage
            file.transferTo(dest);
            System.out.println("Added File");
        }
        else{
            // We don't add the file to prevent overwriting
            System.out.println("File Exists");
        }

        // Returns all the relevant information
        return String.format("TEAM_%s/%s_%s",teamId, fileName, timestamp);
    }


    public File getFile(String filename) throws MalformedURLException {
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("A file name was not given");
        }
        // Finds the file from the storage using the filename
        Path file = storageDir.resolve(filename).normalize();
        Resource resource = new UrlResource(file.toUri());

        if (resource.exists()) {
            // If the file exists, we return the file
            return file.toFile();
        }
        // If the file doesn't exist, we return null
        return null;
    }

    // Request stuff

    /**
     * Unified Request method for all operations
     *\
     * Operations:
     * - JSON: "CREATE", "GET", "GET_ALL", "GET_VERSIONS", "UPDATE", "PATCH", "DELETE", "CLEAR"
     * - Code Files: "UPLOAD_CODE", "DOWNLOAD_CODE", "LIST_CODE_FILES", "DELETE_CODE_FILE", "GET_CODE_VERSIONS"
     *
     * @param operation - Operation type
     * @param teamId - Team ID for file organization
     * @param key - Storage key or filename
     * @param data - Data for JSON operations
     * @return Response map
     */
    public Map<String, Object> Request(String operation, Long teamId, String key, Map<String, Object> data) throws IOException {
        if (operation == null || operation.isEmpty()) {
            throw new IllegalArgumentException("Operation type is required");
        }

        switch (operation.toUpperCase()) {
            // JSON Operations (backward compatible)
            case "CREATE":
                return handleCreate(key, data);
            case "GET":
                return handleGet(key);
            case "GET_ALL":
                return handleGetAll();
            case "GET_VERSIONS":
                return handleGetVersions(key);
            case "UPDATE":
                return handleUpdate(key, data);
            case "PATCH":
                return handlePatch(key, data);
            case "DELETE":
                return handleDelete(key);
            case "CLEAR":
                return handleClear();

            // Code File Operations (team-specific)
            case "LIST_CODE_FILES":
                return handleListCodeFiles(teamId);
            case "DELETE_CODE_FILE":
                return handleDeleteCodeFile(teamId, key);
            case "GET_CODE_VERSIONS":
                return handleGetCodeVersions(teamId, key);

            default:
                throw new IllegalArgumentException("Unknown operation: " + operation);
        }
    }

    /**
     * Overloaded Request method for code file uploads with MultipartFile
     *
     * @param operation - Must be "UPLOAD_CODE" or "DOWNLOAD_CODE"
     * @param teamId - Team ID
     * @param file - MultipartFile for upload
     * @param filename - Filename (for download)
     * @return Response map
     */
    public Map<String, Object> Request(String operation, Long teamId, MultipartFile file, String filename) throws IOException {
        if (operation == null || operation.isEmpty()) {
            throw new IllegalArgumentException("Operation type is required");
        }

        if (teamId == null) {
            throw new IllegalArgumentException("Team ID is required");
        }

        switch (operation.toUpperCase()) {
            case "UPLOAD_CODE":
                return handleUploadCodeFile(teamId, file);

            case "DOWNLOAD_CODE":
                return handleDownloadCodeFile(teamId, filename);

            default:
                throw new IllegalArgumentException("Operation '" + operation + "' not supported with MultipartFile");
        }
    }

    // Handlers

    /**
     * Upload a code file for a specific team with timestamp versioning
     */
    private Map<String, Object> handleUploadCodeFile(Long teamId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        // Extract original filename and extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            originalFilename = "unknown_file";
        }

        // Get file extension
        String extension = "";
        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            extension = originalFilename.substring(lastDotIndex);
            originalFilename = originalFilename.substring(0, lastDotIndex);
        }

        // Create team-specific directory: storage/TEAM_<teamId>/
        Path teamDir = storageDir.resolve("TEAM_" + teamId);
        if (!Files.exists(teamDir)) {
            Files.createDirectories(teamDir);
        }

        // Generate timestamp
        String timestamp = LocalDateTime.now(clock).format(TIMESTAMP_FORMATTER);

        // Create versioned filename: originalName_timestamp.extension
        String versionedFilename = originalFilename + "_" + timestamp + extension;
        Path targetPath = teamDir.resolve(versionedFilename);

        // Save the file
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        // Prepare response
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Code file uploaded successfully");
        response.put("teamId", teamId);
        response.put("originalFilename", file.getOriginalFilename());
        response.put("storedFilename", versionedFilename);
        response.put("path", targetPath.toString());
        response.put("size", file.getSize());
        response.put("contentType", file.getContentType());
        response.put("timestamp", timestamp);
        return response;
    }

    /**
     * Download a specific code file for a team
     */
    private Map<String, Object> handleDownloadCodeFile(Long teamId, String filename) throws IOException {
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("Filename is required");
        }

        Path teamDir = storageDir.resolve("TEAM_" + teamId);
        Path filePath = teamDir.resolve(filename).normalize();

        // Security check - ensure the file is within the team directory
        if (!filePath.startsWith(teamDir)) {
            throw new SecurityException("Access denied: Invalid file path");
        }

        File file = filePath.toFile();
        if (!file.exists() || !file.isFile()) {
            throw new IllegalStateException("File not found: " + filename);
        }

        // Read file content as bytes
        byte[] fileContent = Files.readAllBytes(filePath);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("teamId", teamId);
        response.put("filename", filename);
        response.put("path", filePath.toString());
        response.put("size", file.length());
        response.put("lastModified", new Date(file.lastModified()));
        response.put("file", file);
        response.put("content", fileContent);
        return response;
    }

    /**
     * List all code files for a specific team
     */
    private Map<String, Object> handleListCodeFiles(Long teamId) throws IOException {
        Path teamDir = storageDir.resolve("TEAM_" + teamId);

        if (!Files.exists(teamDir) || !Files.isDirectory(teamDir)) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("teamId", teamId);
            response.put("count", 0);
            response.put("files", new ArrayList<>());
            return response;
        }

        File[] files = teamDir.toFile().listFiles();
        List<Map<String, Object>> fileList = new ArrayList<>();

        if (files != null) {
            for (File file : files) {
                if (file.isFile() && !file.getName().endsWith(JSON_EXTENSION)) {
                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put("filename", file.getName());
                    fileInfo.put("originalName", extractOriginalFilename(file.getName()));
                    fileInfo.put("timestamp", extractCodeFileTimestamp(file.getName()));
                    fileInfo.put("size", file.length());
                    fileInfo.put("extension", getFileExtension(file.getName()));
                    fileInfo.put("lastModified", new Date(file.lastModified()));
                    fileInfo.put("path", file.getAbsolutePath());
                    fileList.add(fileInfo);
                }
            }
        }

        // Sort by timestamp (newest first)
        fileList.sort((a, b) -> {
            String tsA = (String) a.get("timestamp");
            String tsB = (String) b.get("timestamp");
            return tsB.compareTo(tsA);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("teamId", teamId);
        response.put("count", fileList.size());
        response.put("files", fileList);
        return response;
    }

    /**
     * Get all versions of a specific code file for a team
     */
    private Map<String, Object> handleGetCodeVersions(Long teamId, String baseFilename) throws IOException {
        if (baseFilename == null || baseFilename.isEmpty()) {
            throw new IllegalArgumentException("Base filename is required");
        }

        Path teamDir = storageDir.resolve("TEAM_" + teamId);

        if (!Files.exists(teamDir)) {
            throw new IllegalStateException("No files found for team " + teamId);
        }

        // Remove extension from baseFilename if present
        String filenameWithoutExt = baseFilename;
        int lastDot = baseFilename.lastIndexOf('.');
        String extension = "";
        if (lastDot > 0) {
            filenameWithoutExt = baseFilename.substring(0, lastDot);
            extension = baseFilename.substring(lastDot);
        }

        // Find all versions of this file
        final String searchName = filenameWithoutExt;
        final String searchExt = extension;

        File[] files = teamDir.toFile().listFiles((dir, name) -> {
            // Match pattern: filename_timestamp.extension
            return name.startsWith(searchName + "_") &&
                    (searchExt.isEmpty() || name.endsWith(searchExt));
        });

        if (files == null || files.length == 0) {
            throw new IllegalStateException("No versions found for file: " + baseFilename);
        }

        List<Map<String, Object>> versions = new ArrayList<>();
        for (File file : files) {
            Map<String, Object> versionInfo = new HashMap<>();
            versionInfo.put("filename", file.getName());
            versionInfo.put("timestamp", extractCodeFileTimestamp(file.getName()));
            versionInfo.put("size", file.length());
            versionInfo.put("lastModified", new Date(file.lastModified()));
            versionInfo.put("path", file.getAbsolutePath());
            versions.add(versionInfo);
        }

        // Sort by timestamp (oldest to newest)
        versions.sort((a, b) -> {
            String tsA = (String) a.get("timestamp");
            String tsB = (String) b.get("timestamp");
            return tsA.compareTo(tsB);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("teamId", teamId);
        response.put("baseFilename", baseFilename);
        response.put("count", versions.size());
        response.put("versions", versions);
        return response;
    }

    /**
     * Delete a specific code file for a team
     */
    private Map<String, Object> handleDeleteCodeFile(Long teamId, String filename) throws IOException {
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("Filename is required");
        }

        Path teamDir = storageDir.resolve("TEAM_" + teamId);
        Path filePath = teamDir.resolve(filename).normalize();

        // Security check
        if (!filePath.startsWith(teamDir)) {
            throw new SecurityException("Access denied: Invalid file path");
        }

        File file = filePath.toFile();
        if (!file.exists()) {
            throw new IllegalStateException("File not found: " + filename);
        }

        boolean deleted = file.delete();

        Map<String, Object> response = new HashMap<>();
        response.put("success", deleted);
        response.put("message", deleted ? "File deleted successfully" : "Failed to delete file");
        response.put("teamId", teamId);
        response.put("filename", filename);
        return response;
    }

    // ===== JSON OPERATION HANDLERS (same as before) =====

    private Map<String, Object> handleCreate(String key, Map<String, Object> data) throws IOException {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key is required");
        }
        if (data == null) {
            throw new IllegalArgumentException("Data is required");
        }

        String timestamp = LocalDateTime.now(clock).format(TIMESTAMP_FORMATTER);
        String filename = key + "_" + timestamp + JSON_EXTENSION;
        File file = storageDir.resolve(filename).toFile();

        if (!storageDir.toFile().exists()) {
            storageDir.toFile().mkdirs();
        }

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, data);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Item created successfully");
        response.put("key", key);
        response.put("filename", filename);
        response.put("timestamp", timestamp);
        response.put("data", data);
        return response;
    }

    private Map<String, Object> handleGet(String key) throws IOException {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key is required");
        }

        File latestFile = getLatestVersionFile(key);

        if (latestFile == null) {
            throw new IllegalStateException("Key '" + key + "' not found");
        }

        Object data = objectMapper.readValue(latestFile, Object.class);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("key", key);
        response.put("filename", latestFile.getName());
        response.put("data", data);
        return response;
    }

    private Map<String, Object> handleGetAll() throws IOException {
        File dir = storageDir.toFile();

        if (!dir.exists()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", 0);
            response.put("data", new HashMap<>());
            return response;
        }

        Set<String> uniqueKeys = getAllUniqueKeys();
        Map<String, Object> allItems = new HashMap<>();

        for (String key : uniqueKeys) {
            File latestFile = getLatestVersionFile(key);
            if (latestFile != null) {
                Object data = objectMapper.readValue(latestFile, Object.class);
                allItems.put(key, data);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("count", allItems.size());
        response.put("data", allItems);
        return response;
    }

    private Map<String, Object> handleGetVersions(String key) throws IOException {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key is required");
        }

        List<File> versionFiles = getVersionFiles(key);

        if (versionFiles.isEmpty()) {
            throw new IllegalStateException("Key '" + key + "' not found");
        }

        List<Map<String, Object>> versions = new ArrayList<>();

        for (File file : versionFiles) {
            Map<String, Object> versionInfo = new HashMap<>();
            versionInfo.put("filename", file.getName());
            versionInfo.put("timestamp", extractTimestamp(file.getName()));
            versionInfo.put("data", objectMapper.readValue(file, Object.class));
            versions.add(versionInfo);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("key", key);
        response.put("count", versions.size());
        response.put("versions", versions);
        return response;
    }

    private Map<String, Object> handleUpdate(String key, Map<String, Object> data) throws IOException {
        return handleCreate(key, data);
    }

    private Map<String, Object> handlePatch(String key, Map<String, Object> updates) throws IOException {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key is required");
        }
        if (updates == null) {
            throw new IllegalArgumentException("Updates are required");
        }

        File latestFile = getLatestVersionFile(key);

        if (latestFile == null) {
            throw new IllegalStateException("Key '" + key + "' not found");
        }

        Map<String, Object> existing = objectMapper.readValue(latestFile,
                new TypeReference<Map<String, Object>>() {});

        existing.putAll(updates);
        return handleCreate(key, existing);
    }

    private Map<String, Object> handleDelete(String key) throws IOException {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key is required");
        }

        List<File> versionFiles = getVersionFiles(key);

        if (versionFiles.isEmpty()) {
            throw new IllegalStateException("Key '" + key + "' not found");
        }

        int deletedCount = 0;
        for (File file : versionFiles) {
            if (file.delete()) {
                deletedCount++;
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Deleted " + deletedCount + " version(s) of key '" + key + "'");
        response.put("deletedVersions", deletedCount);
        return response;
    }

    private Map<String, Object> handleClear() throws IOException {
        File dir = storageDir.toFile();

        if (!dir.exists()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cleared 0 files from storage");
            return response;
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(JSON_EXTENSION));
        int count = 0;

        if (files != null) {
            for (File file : files) {
                if (file.delete()) {
                    count++;
                }
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Cleared " + count + " file(s) from storage");
        return response;
    }

    // Helper

    private File getLatestVersionFile(String key) {
        List<File> versionFiles = getVersionFiles(key);
        return versionFiles.isEmpty() ? null : versionFiles.get(versionFiles.size() - 1);
    }

    private List<File> getVersionFiles(String key) {
        File dir = storageDir.toFile();

        if (!dir.exists()) {
            return Collections.emptyList();
        }

        File[] files = dir.listFiles((d, name) ->
                name.startsWith(key + "_") && name.endsWith(JSON_EXTENSION)
        );

        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }

        return Arrays.stream(files)
                .sorted(Comparator.comparing(File::getName))
                .collect(Collectors.toList());
    }

    private Set<String> getAllUniqueKeys() {
        File dir = storageDir.toFile();

        if (!dir.exists()) {
            return Collections.emptySet();
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(JSON_EXTENSION));

        if (files == null) {
            return Collections.emptySet();
        }

        return Arrays.stream(files)
                .map(f -> extractKeyFromFilename(f.getName()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private String extractKeyFromFilename(String filename) {
        if (!filename.endsWith(JSON_EXTENSION)) {
            return null;
        }

        String nameWithoutExt = filename.substring(0, filename.length() - JSON_EXTENSION.length());

        int lastUnderscore = nameWithoutExt.lastIndexOf('_');
        if (lastUnderscore > 0) {
            String afterLastUnderscore = nameWithoutExt.substring(lastUnderscore + 1);
            if (afterLastUnderscore.matches("\\d{8}_\\d{6}_\\d{3}")) {
                int secondLastUnderscore = nameWithoutExt.lastIndexOf('_', lastUnderscore - 1);
                if (secondLastUnderscore > 0) {
                    int thirdLastUnderscore = nameWithoutExt.lastIndexOf('_', secondLastUnderscore - 1);
                    if (thirdLastUnderscore > 0) {
                        return nameWithoutExt.substring(0, thirdLastUnderscore);
                    }
                }
            }
        }

        return nameWithoutExt;
    }

    private String extractTimestamp(String filename) {
        String nameWithoutExt = filename.substring(0, filename.length() - JSON_EXTENSION.length());
        int firstUnderscore = nameWithoutExt.indexOf('_');

        if (firstUnderscore > 0) {
            String possibleTimestamp = nameWithoutExt.substring(firstUnderscore + 1);
            if (possibleTimestamp.matches("\\d{8}_\\d{6}_\\d{3}")) {
                return possibleTimestamp;
            }
        }

        return "unknown";
    }

    private String extractOriginalFilename(String versionedFilename) {
        // Remove timestamp and extension to get original name
        int lastUnderscore = versionedFilename.lastIndexOf('_');
        if (lastUnderscore > 0) {
            String beforeUnderscore = versionedFilename.substring(0, lastUnderscore);
            String afterUnderscore = versionedFilename.substring(lastUnderscore + 1);

            // Check if what's after the underscore is a timestamp
            if (afterUnderscore.matches("\\d{8}_\\d{6}_\\d{3}.*")) {
                int extensionDot = versionedFilename.lastIndexOf('.');
                if (extensionDot > lastUnderscore) {
                    return beforeUnderscore + versionedFilename.substring(extensionDot);
                }
                return beforeUnderscore;
            }
        }

        return versionedFilename;
    }

    private String extractCodeFileTimestamp(String filename) {
        // Extract timestamp from versioned filename: originalName_timestamp.extension
        int lastUnderscore = filename.lastIndexOf('_');
        int lastDot = filename.lastIndexOf('.');

        if (lastUnderscore > 0 && lastDot > lastUnderscore) {
            String timestamp = filename.substring(lastUnderscore + 1, lastDot);
            if (timestamp.matches("\\d{8}_\\d{6}_\\d{3}")) {
                return timestamp;
            }
        }

        return "unknown";
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            return filename.substring(lastDot);
        }
        return "";
    }

    public void verifyAccess() {
        return;
    }

    public void deleteFile(String filePath) throws IOException {
        if (filePath == null) {
            throw new IllegalArgumentException("File path is missing");
        }
        File deletedFile = new File(filePath);
        if (deletedFile.exists()) {
            deletedFile.delete();
        }
        else {
            System.out.println("File does not exist");
        }
    }
}