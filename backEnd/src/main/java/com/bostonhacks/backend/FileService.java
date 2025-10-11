package com.bostonhacks.backend;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service // Mark as a Spring service
public class FileService {

    private final Path uploadDirectory;
    // The SensitiveInfoDetector is not directly needed here for this specific setup,
    // as it will be injected into the Controller directly.

    public FileService() throws IOException {
        this.uploadDirectory = Paths.get("uploads").toAbsolutePath().normalize();
        if (!Files.exists(this.uploadDirectory)) {
            Files.createDirectories(this.uploadDirectory);
            System.out.println("Upload directory created: " + this.uploadDirectory);
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
    public Path storeFile(MultipartFile file) throws IOException {
        String originalFilename = Objects.requireNonNull(file.getOriginalFilename());
        String cleanedFilename = originalFilename.substring(originalFilename.lastIndexOf('/') + 1)
            .substring(originalFilename.lastIndexOf('\\') + 1);

        String fileExtension = "";
        int dotIndex = cleanedFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            fileExtension = cleanedFilename.substring(dotIndex);
            cleanedFilename = cleanedFilename.substring(0, dotIndex);
        }
        String uniqueFileName =
            cleanedFilename + "_" + UUID.randomUUID() + fileExtension;

        Path targetLocation = this.uploadDirectory.resolve(uniqueFileName);

        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        return targetLocation;
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
}