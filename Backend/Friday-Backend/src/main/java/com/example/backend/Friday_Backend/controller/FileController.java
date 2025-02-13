package com.example.backend.Friday_Backend.controller;

import com.example.backend.Friday_Backend.service.GeminiTextExtract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api")
public class FileController {
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/png", "image/jpeg", "image/jpg");
    private static final String PDF_TYPE = "application/pdf";

    private final GeminiTextExtract geminiTextExtract;

    @Autowired
    public FileController(GeminiTextExtract geminiTextExtract) {
        this.geminiTextExtract = geminiTextExtract;
    }

    @PostMapping("/extract/image")
    public ResponseEntity<Map<String, Object>> extractTextFromImage(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        String fileType = file.getContentType();

        if (fileType == null || !ALLOWED_IMAGE_TYPES.contains(fileType)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid file type. Only PNG, JPEG, and JPG images are allowed."));
        }

        try {
            String extractedText = geminiTextExtract.extractTextFromImage(file);

            response.put("status", "success");
            response.put("message", "Text extracted successfully from image.");
            response.put("filename", file.getOriginalFilename());
            response.put("size", file.getSize() + " bytes");
            response.put("type", fileType);
            response.put("extracted_text", extractedText);

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error processing the image file."));
        }
    }

    @PostMapping("/extract/pdf")
    public ResponseEntity<Map<String, Object>> extractTextFromPdf(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        String fileType = file.getContentType();

        if (fileType == null || !fileType.equals(PDF_TYPE)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid file type. Only PDF files are allowed."));
        }

        try {
            String extractedTextJson = geminiTextExtract.extractTextFromPdf(file);

            response.put("status", "success");
            response.put("message", "Text extracted successfully from PDF.");
            response.put("filename", file.getOriginalFilename());
            response.put("size", file.getSize() + " bytes");
            response.put("type", fileType);
            response.put("extracted_text", extractedTextJson);

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error processing the PDF file."));
        }
    }
}
