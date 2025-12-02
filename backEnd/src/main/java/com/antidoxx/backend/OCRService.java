package com.antidoxx.backend;

import java.io.FileInputStream;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class OCRService {
    private static final Logger logger = LoggerFactory.getLogger(OCRService.class);
    private static final List<String> SUPPORTED_EXTENSIONS = Arrays.asList(".png", ".jpg", ".jpeg", ".tiff", ".bmp", ".gif");

    public OCRService() {}

    public static String doOCR(File imageFile) {
        String apiKey = System.getenv("OCR_API_KEY");
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.error("OCR_API_KEY environment variable is not set. Cannot perform OCR.");
            throw new IllegalStateException("OCR_API_KEY environment variable is required but not set. " +
                "Please set the OCR_API_KEY environment variable with your OCR.space API key.");
        }

        try {
            byte[] bytes = new byte[(int) imageFile.length()];
            try (FileInputStream fis = new FileInputStream(imageFile)) {
                fis.read(bytes);
            }

            String base64Image = java.util.Base64.getEncoder().encodeToString(bytes);

            RestTemplate restTemplate = new RestTemplate();
            String url = "https://api.ocr.space/parse/image";

            HttpHeaders headers = new HttpHeaders();
            headers.add("apikey", apiKey);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("base64Image", "data:image/jpeg;base64," + base64Image);
            body.add("language", "eng");

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);

            if (response.getBody() == null) {
                return "";
            }

            Map<String, Object> responseBody = response.getBody();
            logger.info("Response Body: " + responseBody);

            List<Map<String, Object>> parsedResults = (List<Map<String, Object>>) responseBody.get("ParsedResults");

            if (parsedResults == null || parsedResults.isEmpty()) {
                return "";
            }

            StringBuilder result = new StringBuilder();

            for (Map<String, Object> parsedResult : parsedResults) {
                result.append(parsedResult.get("ParsedText"));
            }

            return result.toString();
        } catch (IOException e) {
            logger.error("Error reading image file: {}", imageFile.getAbsolutePath(), e);
            return "";
        } catch (Exception e) {
            logger.error("Error calling OCR.space API for file: {}", imageFile.getAbsolutePath(), e);
            return "";
        }
    }

    public static String extractTextFromImage(File imageFile) {
        if (imageFile == null) {
            throw new IllegalArgumentException("Image file cannot be null");
        }

        if (!imageFile.exists()) {
            throw new IllegalArgumentException("Image file does not exist: " + imageFile.getAbsolutePath());
        }

        if (!imageFile.isFile()) {
            throw new IllegalArgumentException("Path is not a file: " + imageFile.getAbsolutePath());
        }

        if (!isValidImageFormat(imageFile)) {
            throw new IllegalArgumentException("Unsupported image format. Supported formats: " + SUPPORTED_EXTENSIONS);
        }

        logger.debug("Processing OCR for file: {}", imageFile.getAbsolutePath());
        String extractedText = doOCR(imageFile);
        logger.debug("OCR completed successfully for file: {}", imageFile.getAbsolutePath());
        return extractedText != null ? extractedText.trim() : "";
    }

    private static boolean isValidImageFormat(File imageFile) {
        String fileName = imageFile.getName().toLowerCase();
        return SUPPORTED_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }
}
