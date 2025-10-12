package com.bostonhacks.backend;

import com.google.genai.types.GenerateContentResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@RestController
public class RequestController {
    private final StorageHandler storageHandler;
    private final SensitiveInfoDetector sensitiveInfoDetector;

    public RequestController(StorageHandler storageHandler, SensitiveInfoDetector sensitiveInfoDetector) {
        this.storageHandler = storageHandler;
        this.sensitiveInfoDetector = sensitiveInfoDetector;
    }

    /**
     * takes in raw text advice
     *
     * @param input raw text advice from the frontend chat interface
     * @return text advice
     */
    @GetMapping("/text")
    public String getText(@RequestBody String input) {
        try {
            // Step 1: Save input string as a temporary .txt file
            Path tempFile = Files.createTempFile("user-input-", ".txt");
            Files.writeString(tempFile, input, StandardCharsets.UTF_8);
            System.out.println("📄 File created: " + tempFile.toAbsolutePath());

            // Step 2: Call Gemini to analyze the text file
            String advice = getTextAdvice(tempFile.toString());

            // Step 3: Return Gemini’s text output
            return advice;

        } catch (IOException e) {
            e.printStackTrace();
            return "Error: Unable to process text - " + e.getMessage();
        }
    }

    /**
     * takes in a TEXTUAL file (*.txt, *.md) from the frontend interface
     *
     * @param filename raw text advice from the frontend chat interface
     * @return text advice
     */
    @GetMapping("/text-advice")
    public String getTextAdvice(@RequestParam("file") String filename) {
        // fixme return str
        return Gemini.getInstance().getGemini().models.generateContent(
            "gemini-2.5-flash",
            "Please search this text file and examine if there's any personally identifiable information.",
            null
        ).toString();
    }

    @PostMapping("/upload-file")
    public ResponseEntity<Map<String, Object>> uploadFile(
        @RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        if (file.isEmpty()) {
            response.put("success", false);
            response.put("message", "Please select a file to upload.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".txt")) {
            response.put("success", false);
            response.put("message", "Invalid file name.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        try {
            // 3. Store the file to a temporary location on the server
            // This is necessary because SensitiveInfoDetector works with a Path
            Path storedFilePath = storageHandler.storeFile(file);

            // 4. Read the content from the stored file for processing and preview
            String fileContent = storageHandler.readFileContent(storedFilePath);

            // 5. Detect Sensitive Information using the dedicated service
            List<String> sensitiveInfoDetections =
                sensitiveInfoDetector.detectSensitiveInfo(storedFilePath);
            boolean hasSensitiveInfo = !sensitiveInfoDetections.isEmpty();

            // 6. Populate the response map with file metadata
            response.put("success", true); // The upload itself was successful
            response.put("originalFilename", originalFilename);
            response.put("storedFilename", storedFilePath.getFileName().toString());
            response.put("fileSize", file.getSize());
            response.put("fileType", file.getContentType());
            response.put("filePath",
                storedFilePath.toString()); // The server-side path where it was stored

            // Include a preview of the file content
            response.put("contentPreview",
                fileContent.substring(0, Math.min(fileContent.length(), 500)) +
                    (fileContent.length() > 500 ? "..." : ""));

            // --- 7. Customize the User Message and Flags based on Sensitive Info Detection ---
            response.put("hasSensitiveInfo", hasSensitiveInfo);

            if (hasSensitiveInfo) {
                // Construct a detailed warning message for the user
                StringBuilder messageBuilder = new StringBuilder();
                messageBuilder.append("WARNING: Sensitive information was detected in your file '")
                    .append(originalFilename).append("'.\n");
                messageBuilder.append("Detected items include:\n");
                // Iterate through detected items and add them to the message
                sensitiveInfoDetections.forEach(
                    detection -> messageBuilder.append("- ").append(detection).append("\n"));
                messageBuilder.append("\nPlease review your file for security concerns.");

                response.put("message", messageBuilder.toString());
                response.put("sensitiveInfoDetails",
                    sensitiveInfoDetections); // Provide the raw list of detections
                // Depending on your policy, you might return HttpStatus.FORBIDDEN here
                // For now, we'll return OK but with a clear warning in the message.
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                // No sensitive information found
                response.put("message", "File '" + originalFilename +
                    "' uploaded successfully and no sensitive information was detected.");
                response.put("sensitiveInfoDetails", "No sensitive information found.");
                return new ResponseEntity<>(response, HttpStatus.OK);
            }

        } catch (IOException e) {
            // Handle file I/O errors during storage or reading
            System.err.println(
                "Error processing file upload for '" + originalFilename + "': " + e.getMessage());
            e.printStackTrace(); // Log the full stack trace for debugging
            response.put("success", false);
            response.put("message", "Failed to upload or process file '" + originalFilename +
                "' due to a server error: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            // Catch any other unexpected runtime exceptions
            System.err.println(
                "An unexpected error occurred during file upload for '" + originalFilename + "': " +
                    e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "An unexpected error occurred: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @PostMapping("/upload-image")
    public String uploadImage(@RequestParam("file") MultipartFile file,
                              RedirectAttributes redirectAttributes) {
        return "";
    }

    @GetMapping("/image-advice")
    public GenerateContentResponse getImageAdvice(@RequestParam("file") String filename) {
        // fixme return str
        return Gemini.getInstance().getGemini().models.generateContent("gemini-2.5-flash",
            "Please search this image and examine if there's any personally identifiable information.",
            null);
    }
}
