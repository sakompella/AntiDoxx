package com.antidoxx.backend;

import org.springframework.stereotype.Service;

import com.google.genai.Client;

/**
 * Automatically reads the API key from environment variables:
 * - GOOGLE_API_KEY
 * - GEMINI_API_KEY
 * 
 * If both are set, GOOGLE_API_KEY takes precedence.
 * 
 * Ensure you set one of these environment variables before running the application.
 * Get your API key at: https://ai.google.dev/
 */
@Service
public class Gemini {
    private final Client client;

    public Gemini() {
        // The Client constructor automatically picks up the API key from environment variables
        // No need to explicitly pass the API key here
        this.client = new Client();
    }

    public Client getGemini() {
        return client;
    }
}
