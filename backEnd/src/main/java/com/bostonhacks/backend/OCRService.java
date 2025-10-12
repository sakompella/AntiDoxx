package com.bostonhacks.backend;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class OCRService {

    private final ITesseract tesseract;

    public OCRService() {
        tesseract = new Tesseract();
        // Path to tessdata folder (contains language data)
        tesseract.setDatapath("tessdata");
        tesseract.setLanguage("eng"); // English language
    }

    public String extractTextFromImage(File imageFile) throws TesseractException {
        return tesseract.doOCR(imageFile);
    }
}