package com.bostonhacks.backend;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service // Mark as a Spring service for dependency injection
public class SensitiveInfoDetector {

    // --- Define Regular Expression Patterns for Sensitive Data ---
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");

    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile(
        "\\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|6(?:011|5[0-9]{2})[0-9]{12}|3[47][0-9]{13}|3(?:0[0-5]|[68][0-9])[0-9]{11}|(?:2131|1800|35\\d{3})\\d{11})\\b");

    private static final Pattern PHONE_PATTERN =
        Pattern.compile("\\b(?:\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4})\\b");

    private static final Pattern SSN_PATTERN =
        Pattern.compile("\\b(?!000|666)[0-8][0-9]{2}-(?!00)[0-9]{2}-(?!0000)[0-9]{4}\\b");

    private static final Pattern IP_ADDRESS_PATTERN = Pattern.compile(
        "\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b");

    private final List<SensitivePattern> patterns;

    public SensitiveInfoDetector() {
        patterns = new ArrayList<>();
        patterns.add(new SensitivePattern("Email Address", EMAIL_PATTERN));
        patterns.add(new SensitivePattern("Credit Card Number", CREDIT_CARD_PATTERN));
        patterns.add(new SensitivePattern("Phone Number", PHONE_PATTERN));
        patterns.add(new SensitivePattern("Social Security Number", SSN_PATTERN));
        patterns.add(new SensitivePattern("IP Address (IPv4)", IP_ADDRESS_PATTERN));
    }

    /**
     * Scans the content of a text file for predefined sensitive information patterns.
     *
     * @param filePath The Path to the text file.
     * @return A list of strings, each describing a detected sensitive item, or an empty list if none found.
     * @throws IOException If there's an error reading the file.
     */
    public List<String> detectSensitiveInfo(Path filePath) throws IOException {
        List<String> detectedItems = new ArrayList<>();

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath) ||
            !Files.isReadable(filePath)) {
            throw new IOException(
                "File does not exist, is not a regular file, or is not readable: " + filePath);
        }

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                for (SensitivePattern sensitivePattern : patterns) {
                    Matcher matcher = sensitivePattern.pattern.matcher(line);
                    while (matcher.find()) {
                        String foundData = matcher.group();
                        detectedItems.add(
                            "Line " + lineNumber + ": " + sensitivePattern.name + " detected -> '" +
                                foundData + "'");
                    }
                }
            }
        }
        return detectedItems;
    }

    private record SensitivePattern(String name, Pattern pattern) {
    }
}