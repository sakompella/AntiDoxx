package com.bostonhacks.backend;

import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@CrossOrigin(origins = "*")
@RestController
public class RequestController {
    private final StorageHandler storageHandler;
    private final SensitiveInfoDetector sensitiveInfoDetector;
    private final String TEXT_PROMPT =
        "Please analyze the following for any personally identifiable information (PII) " +
            "such as names, addresses, phone numbers, email addresses, social security numbers, " +
            "credit card numbers, IP addresses, and other potentially sensitive data. " +
            "Provide a clear, structured analysis highlighting any findings. " +
            "Keep the response concise and focused solely on identifying sensitive information. " +
            "Do NOT provide any additional commentary or suggestions beyond the identification of PII. " +
            "If no sensitive information is found, simply state 'No sensitive information detected.' " +
            "Respond in plaintext format, without markdown formatting.";

    Logger logger = LoggerFactory.getLogger(RequestController.class);

    public RequestController(StorageHandler storageHandler,
                             SensitiveInfoDetector sensitiveInfoDetector) {
        this.storageHandler = storageHandler;
        this.sensitiveInfoDetector = sensitiveInfoDetector;
    }

    private String analyzeWithGemini(Content data) {
        Content[] contentArr = {data, Content.fromParts(Part.fromText(TEXT_PROMPT))};
        var response = Gemini.getInstance().getGemini().models.generateContent("gemini-2.5-flash",
            Arrays.asList(contentArr), null);
        return extractContentFromResponse(response);
    }

    private String analyzeTextWithGemini(String content) {
        Content data = Content.fromParts(Part.fromBytes(content.getBytes(), "text/markdown"));
        return analyzeWithGemini(data);
    }

    private String analyzeImageWithGemini(Path filePath) throws IOException {
        byte[] imageBytes = Files.readAllBytes(filePath);
        Content data =
            Content.fromParts(Part.fromBytes(imageBytes, storageHandler.mimeType(filePath)));
        return analyzeWithGemini(data);
    }

    private String extractContentFromResponse(GenerateContentResponse response) {
        if (response == null) {
            return "No analysis available.";
        }

        try {

            // Extract text content from the response by parsing the string representation
            String responseStr = response.text();

            // Find the text content in the response string
            // This is a simple approach to extract the actual content from the response
            if (responseStr.contains("text=")) {
                int startIndex = responseStr.indexOf("text=") + 5;
                int endIndex = responseStr.indexOf("}", startIndex);
                if (endIndex == -1) {
                    endIndex = responseStr.length();
                }
                String content = responseStr.substring(startIndex, endIndex);
                // Clean up the extracted content
                content = content.replaceAll("^[\"']|[\"']$", ""); // Remove quotes
                return content.trim();
            }

            while (responseStr.contains("Optional[")) {
                int begin = responseStr.lastIndexOf("Optional[");
                int end = responseStr.indexOf("]", begin);
                responseStr = responseStr.substring(begin + 9, end);
            }

            logger.info("Response Body: " + responseStr);

            return responseStr.trim();
        } catch (Exception e) {
            logger.warn("Error extracting content from response: {}", e.getMessage());
            return response.toString();
        }
    }

    private String analyzeContent(String content, String contentType) {
        StringBuilder result = new StringBuilder();

        // First check for sensitive information using our local detector
        java.util.List<String> sensitiveItems = sensitiveInfoDetector.detectSensitiveInfo(content);

        if (!sensitiveItems.isEmpty()) {
            result.append("**SENSITIVE INFORMATION DETECTED**\n\n");
            result.append("The following sensitive information was found in your ")
                .append(contentType).append(":\n\n");
            for (String item : sensitiveItems) {
                result.append("- ").append(item).append("\n");
            }
            result.append("\n---\n\n");
        }

        // Then get AI analysis
        String geminiAnalysis = analyzeTextWithGemini(content);
        result.append("**AI Analysis**\n\n").append(geminiAnalysis);

        return result.toString();
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
    public Map<String, Serializable> getTextAdvice(@RequestParam("text") String input) {
        try {
            String analysisResult = analyzeContent(input, "text input");
            return Map.of("code", 0, "message", analysisResult);
        } catch (Exception e) {
            return Map.of("code", -1, "message", "Error: " + e.getMessage());
        }
    }

    /**
     * Unified endpoint that handles both text files and images for PII analysis
     *
     * @param filename file name from the frontend interface
     * @return analysis advice based on file type
     */
    @GetMapping("/file-advice")
    public ResponseEntity<Map<String, Serializable>> getFileAdvice(
        @RequestParam("filename") String filename) {
        int code = 0;
        try {
            Path filePath = storageHandler.fetchFile(filename);
            String mimeType = storageHandler.mimeType(filePath);
            String analysisResult;

            if (mimeType.contains("text")) {
                String fileContent = Files.readString(filePath);
                analysisResult = analyzeContent(fileContent, "text file");
                logger.info("Analyzed text file: {}", filename);
            } else if (mimeType.contains("image")) {
                // Use OCR to extract text from image, then analyze the text
                String extractedText = OCRService.extractTextFromImage(filePath.toFile());
                if (!extractedText.trim().isEmpty()) {
                    analysisResult = analyzeContent(extractedText, "image text");
                    logger.info("Analyzed image file with OCR: {}", filename);
                } else {
                    code = 1;
                    StringBuilder result = new StringBuilder();
                    result.append("**Image Analysis**\n\n");
                    result.append(
                        "No readable text was found in the image using OCR. Analyzing image directly...\n\n");
                    result.append("---\n\n");
                    String directAnalysis = analyzeImageWithGemini(filePath);
                    result.append("**AI Visual Analysis**\n\n").append(directAnalysis);
                    analysisResult = result.toString();
                    logger.info("Analyzed image file directly: {}", filename);
                }
            } else {
                logger.error("Unsupported file type: {} for file: {}", mimeType, filename);
                return new ResponseEntity<>(
                    Map.of("code", -1, "message", "Error: Unsupported file type - " + mimeType),
                    HttpStatus.BAD_REQUEST);
            }

            return new ResponseEntity<>(Map.of("code", code, "message", analysisResult),
                HttpStatus.OK);

        } catch (IOException e) {
            logger.error("Error reading file: {}", filename, e);
            return new ResponseEntity<>(
                Map.of("code", -1, "message", "Error: Unable to read file - " + e.getMessage()),
                HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Error analyzing file: {}", filename, e);
            return new ResponseEntity<>(
                Map.of("code", -1, "message", "Error analyzing file: " + e.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR);
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
        @RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        if (file.isEmpty()) {
            response.put("success", false);
            response.put("code", -1);
            response.put("message", "Please select a file to upload.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            response.put("success", false);
            response.put("code", -2);
            response.put("message", "Invalid file name.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        try {
            // 3. Store the file to a temporary location on the server
            // This is necessary because SensitiveInfoDetector works with a Path
            Path storedFilePath = storageHandler.storeFile(file, logger);

            // Populate the response map with file metadata
            response.put("success", true); // The upload itself was successful
            response.put("code", 0);
            response.put("filename", storedFilePath.getFileName().toString());
            // No sensitive information found
            response.put("message", "File '" + originalFilename + "' uploaded successfully.");
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (IOException e) {
            // Handle file I/O errors during storage or reading
            logger.error("Error processing file upload for '{}': {}", originalFilename,
                e.getMessage(), e);
            response.put("success", false);
            response.put("code", -3);
            response.put("message", "Failed to upload or process file '" + originalFilename +
                "' due to a server error: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            // Catch any other unexpected runtime exceptions
            logger.error("An unexpected error occurred during file upload for '{}': {}",
                originalFilename, e.getMessage(), e);
            response.put("success", false);
            response.put("code", -4);
            response.put("message", "An unexpected error occurred: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
