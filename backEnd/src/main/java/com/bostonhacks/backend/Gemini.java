package com.bostonhacks.backend;

import com.google.genai.Client;

public final class Gemini {
  private static Gemini INSTANCE;
  private final static String API_KEY = "AIzaSyD6ev0oDBpe7_424-Jqkl4hZiJVx6cj4-o";
  private final static Client client = new Client();

  public Client getGemini() {
    return client;
  }

  private Gemini() {}

  public static Gemini getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new Gemini();
    }
    return INSTANCE;
  }
}
