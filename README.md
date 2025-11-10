# AntiDoxx - Sensitive Information Detector

[![BostonHacks 2025](https://img.shields.io/badge/BostonHacks-2025-red)](https://devpost.com/software/anti-doxx)

**Anti-Doxx stops doxxing by intelligently scanning your files and text for PII.**

> **Anti-Doxx** was originally a submission to **BostonHacks 2025**.

## Overview

AntiDoxx is a web application designed to protect users from accidentally sharing sensitive personal information. Built during BostonHacks 2025 (Boston University's largest annual student-run hackathon), this tool analyzes files and text input to detect and alert users about potentially exposed Personally Identifiable Information (PII) such as:

- Email addresses
- Credit card numbers
- Phone numbers
- Social Security Numbers (SSN)
- IP addresses
- Names, addresses, and other personally identifiable data

The application uses a combination of regex-based pattern matching and AI-powered analysis (Google Gemini) to provide comprehensive detection of sensitive information.

## Features

- **File Upload Analysis**: Upload any file and scan it for sensitive information
- **Text Input Analysis**: Paste text directly and analyze it for PII
- **Dual Detection Methods**: Regex pattern matching and AI-powered analysis using Google Gemini
- **OCR Text Extraction**: Extract and analyze text from images
- **Dark Mode Support**: Toggle between light and dark themes
- **User-Friendly Interface**: Simple, intuitive web interface for easy access

## Technology Stack

### Backend

- **Language**: Java 21
- **Framework**: Spring Boot 3.5.6
- **Build Tool**: Gradle
- **Key Technologies**:
  - Google Gemini AI (google-genai:1.0.0) for intelligent PII detection
  - OCR.space API for image text extraction
  - REST API architecture
- **Features**:
  - Service-oriented architecture with dependency injection
  - Regex-based pattern detection
  - Google Gemini API integration for AI analysis
  - OCR text extraction from images
  - File storage and processing

### Frontend

- **Languages**: HTML, CSS, JavaScript
- **Features**:
  - Tab-based interface for file and text uploads
  - Real-time status feedback
  - Theme toggle (light/dark mode)
  - Responsive design

## Getting Started

### Prerequisites

- **Java 21+** - Required for Spring Boot 3.5.6
- **Gradle** - For building the backend
- Modern web browser for the frontend

### Environment Setup

#### 1. Configure Environment Variables

The application requires API keys for OCR and Gemini services. 

**Create environment variables:**

```bash
# On macOS/Linux
export OCR_API_KEY="your_ocr_api_key_here"
export GOOGLE_API_KEY="your_gemini_api_key_here"

# On Windows (PowerShell)
$env:OCR_API_KEY="your_ocr_api_key_here"
$env:GOOGLE_API_KEY="your_gemini_api_key_here"
```

**Getting API Keys:**

- **OCR API Key**: <https://ocr.space/ocrapi>
- **Gemini API Key**: <https://ai.google.dev/>


### Installation & Build

```bash
cd backEnd
./gradlew bootRun
```

The backend will start on `http://localhost:8000`.

#### Frontend Installation

The frontend is a static web application. You have several options:

```bash
cd frontEnd
# Any http server works!
npx http-server -p 8080
```

## Future Possible Improvements

- [ ] Add support for more file formats (PDF, Word documents, etc.)
- [ ] Add batch file processing
- [ ] Add customizable sensitivity levels
- [ ] Support for multiple languages
- [ ] Integration with popular sharing platforms

**[View on Devpost](https://devpost.com/software/anti-doxx)**
