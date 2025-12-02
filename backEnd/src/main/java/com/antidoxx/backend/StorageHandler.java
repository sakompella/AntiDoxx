package com.antidoxx.backend;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StorageHandler {

    private final Path storageDirectory;

    public StorageHandler() throws IOException {
        String tmpDir = System.getProperty("java.io.tmpdir");
        this.storageDirectory =
            Paths.get(tmpDir, "antidoxx-uploads").toAbsolutePath().normalize();
        if (!Files.exists(storageDirectory)) {
            Files.createDirectories(storageDirectory);
            System.out.println("Upload directory created: " + this.storageDirectory);
        }
    }

    /**
     * Stores the uploaded file to the designated upload directory.
     * Generates a unique filename to prevent overwrites.
     *
     * @param file The MultipartFile received from the frontend.
     * @return The Path to the saved file.
     * @throws IOException If there's an error during file storage.
     */
    public Path storeFile(MultipartFile file, Logger logger) throws IOException {
        String cleanedFilename = sanitizeFilename(file.getOriginalFilename());

        Path targetLocation = this.storageDirectory.resolve(cleanedFilename);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        logger.info("File uploaded successfully: {}", targetLocation.toAbsolutePath());

        return targetLocation;
    }

    private String sanitizeFilename(String filename) {
        StringBuilder sanitized = new StringBuilder();

        if (filename == null || filename.isEmpty()) {
            sanitized.append("unnamed");
        } else {
            for (char c : filename.toCharArray()) {
                if (Character.isLetterOrDigit(c)) {
                    sanitized.append(c);
                } else if (Character.isWhitespace(c)) {
                    sanitized.append('_');
                } else if (c == '.' || c == '-') {
                    // Preserve dots (for extensions) and hyphens
                    sanitized.append(c);
                } else {
                    sanitized.append('_');
                }
            }
        }

        String fileExtension = "";

        // returns -1 if there is no dot in the filename
        int dotIndex = sanitized.lastIndexOf(".");

        if (dotIndex > 0) {
            fileExtension = sanitized.substring(dotIndex);
            sanitized.delete(dotIndex, sanitized.length());
        }

        return sanitized + "_" + UUID.randomUUID() + fileExtension;
    }

    public Path storeFile(String filename, byte[] content, Logger logger) throws IOException {
        String cleanedFilename = sanitizeFilename(filename);
        Path filePath = storageDirectory.resolve(cleanedFilename);
        Files.write(filePath, content);
        logger.info("File uploaded successfully: {}", filePath.toAbsolutePath());

        return filePath;
    }

    /**
     * Reads the content of a stored text file.
     *
     * @param filePath The Path to the stored file.
     * @return The content of the file as a String.
     * @throws IOException If there's an error reading the file.
     */
    public String readFileContent(Path filePath) throws IOException {
        return Files.readString(filePath, StandardCharsets.UTF_8);
    }

    public void deleteFile(Path filename) throws IOException {
        Path filePath = storageDirectory.resolve(filename);
        Files.deleteIfExists(filePath);
    }

    public String mimeType(Path path) throws IOException {
        return Files.probeContentType(path);
    }

    public Path fetchFile(String filename) throws IOException {
        Path targetLocation = this.storageDirectory.resolve(filename);
        if (Files.exists(targetLocation)) {
            return targetLocation;
        } else {
            System.out.println("File not found: " + filename);
            return null;
        }
    }
}

