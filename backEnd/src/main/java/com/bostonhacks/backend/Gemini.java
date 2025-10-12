package com.bostonhacks.backend;

import com.google.genai.Client;

public final class Gemini {
    private final static String API_KEY = "AIzaSyD6ev0oDBpe7_424-Jqkl4hZiJVx6cj4-o";
    private final static Client client = new Client();
    private static Gemini INSTANCE;

    private Gemini() {}

    public static synchronized Gemini getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Gemini();
        }
        return INSTANCE;
    }

    public Client getGemini() {
        return client;
    }
}
