package com.bostonhacks.backend;

import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@CrossOrigin(origins = "*")
@RestController
public class RequestController {
    private final StorageHandler storageHandler;
    private final SensitiveInfoDetector sensitiveInfoDetector;

    public RequestController(StorageHandler storageHandler,
                             SensitiveInfoDetector sensitiveInfoDetector) {
        this.storageHandler = storageHandler;
        this.sensitiveInfoDetector = sensitiveInfoDetector;
    }

    private String analyzeTextWithGemini(String content) {
        try {
            String prompt =
                "Please analyze this text and check for personally identifiable information (PII)";
            Content[] contentArr =
                {Content.fromParts(Part.fromBytes(content.getBytes(), "text/markdown")),
                    Content.fromParts(Part.fromText(prompt))};
            var response =
                Gemini.getInstance().getGemini().models.generateContent("gemini-2.5-flash",
                    Arrays.asList(contentArr), null);
            return response.toString();
        } catch (Exception e) {
            return "Error analyzing text: " + e.getMessage();
        }
    }

    /**
     * TESTING METHOD!!
     * echos whatever you sent in
     */
    @GetMapping("/echo")
    public String echo(@RequestParam("text") String text) {
        return text;
    }

    /**
     * takes in raw text advice
     *
     * @param input raw text advice from the frontend chat interface
     * @return text advice
     */
    @GetMapping("/text-advice")
    public String getTextAdvice(@RequestBody String input) {
        try {
            // Step 1: Save input string as a temporary .txt file
            Path tempFile = StorageHandler.getInstance().fetchFile(input);
            Files.writeString(tempFile, input, StandardCharsets.UTF_8);
            System.out.println("File created: " + tempFile.toAbsolutePath());

            // Step 2: Analyze the text directly
            return analyzeTextWithGemini(input);

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
    @GetMapping("/textfile-advice")
    public String getTextFile(@RequestParam("file") String filename) {
        try {
            // 1. Read the file contents as text
            Path filePath = Paths.get("uploads", filename);
            String fileContent = Files.readString(filePath);

            // 2. Analyze the file content
            return analyzeTextWithGemini(fileContent);

        } catch (IOException e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    /*
    error:
    {
      "success": false,
      "code": -1 // no file uploaded
      OR
      "code": -2 // invalid file type
      "code": -3 | -4  // check message
    }

    success:
    {
      "success": true,
      "code": 0,
      "message": "..."
    }
     */
    @PostMapping("/upload-file")
    public ResponseEntity<Map<String, Object>> uploadFile(
        Model model,
        @RequestParam("file") MultipartFile file
    ) {
        Map<String, Object> response = new HashMap<>();
        if (file.isEmpty()) {
            response.put("success", false);
            response.put("code", -1);
            response.put("message", "Please select a file to upload.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".txt")) {
            response.put("success", false);
            response.put("code", -2);
            response.put("message", "Invalid file name.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        try {
            // 3. Store the file to a temporary location on the server
            // This is necessary because SensitiveInfoDetector works with a Path
            Path storedFilePath = storageHandler.storeFile(file);

            // Populate the response map with file metadata
            response.put("success", true); // The upload itself was successful
            response.put("code", 0);
            response.put("filename", storedFilePath.getFileName().toString());
            // No sensitive information found
            response.put("message", "File '" + originalFilename +
                "' uploaded successfully.");
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (IOException e) {
            // Handle file I/O errors during storage or reading
            System.err.println(
                "Error processing file upload for '" + originalFilename + "': " + e.getMessage());
            e.printStackTrace(); // Log the full stack trace for debugging
            response.put("success", false);
            response.put("code", -3);
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
            response.put("code", -4);
            response.put("message", "An unexpected error occurred: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/image-advice")
    public String getImageAdvice(@RequestParam("file") String filename) {
        try {
            Path filePath = storageHandler.fetchFile(filename);
            if (filePath == null) {
                return "Error: File not found - " + filename;
            }

            String prompt = "Please search this image and examine if there's any personally identifiable information.";
            Content[] contentArr = {
                Content.fromParts(Part.fromText(prompt))
            };
            GenerateContentResponse response = Gemini.getInstance().getGemini().models.generateContent(
                "gemini-2.5-flash",
                Arrays.asList(contentArr),
                null
            );
            return response.toString();
        } catch (Exception e) {
            return "Error analyzing image: " + e.getMessage();
        }
    }
}
