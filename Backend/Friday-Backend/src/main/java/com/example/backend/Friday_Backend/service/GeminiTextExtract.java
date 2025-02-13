package com.example.backend.Friday_Backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.ImageType;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

@Service
public class GeminiTextExtract {

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=AIzaSyBMQHpzT8Qi0pBzXltd38CKv90v9r8167o";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String extractTextFromImage(MultipartFile file) throws IOException {
        String jsonResponse = sendImageToAI(file.getBytes(), file.getContentType());
        return parseAiResponse(jsonResponse);
    }

    public String extractTextFromPdf(MultipartFile file) throws IOException {
        Queue<byte[]> imageQueue = convertPdfToImages(file);
        StringBuilder extractedText = new StringBuilder();

        while (!imageQueue.isEmpty()) {
            byte[] imageData = imageQueue.poll();
            String jsonResponse = sendImageToAI(imageData, "image/png");
            extractedText.append(parseAiResponse(jsonResponse)).append("\n");
        }

        return extractedText.toString().trim();
    }

    private Queue<byte[]> convertPdfToImages(MultipartFile file) throws IOException {
        Queue<byte[]> imageQueue = new LinkedList<>();
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(i, 300, ImageType.RGB);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "png", baos);
                imageQueue.add(baos.toByteArray());
            }
        }
        return imageQueue;
    }

    private String sendImageToAI(byte[] imageData, String mimeType) {
        String base64Image = Base64.getEncoder().encodeToString(imageData);

        String requestBody = """
            {
              "contents": [{
                "parts": [
                  {"text": "Extract all text from the provided image, including visible text in any form (code, marketing copy, scripts, speeches, etc.).  Return the extracted text as a single string.  Preserve the original formatting and layout of the text as much as possible, including line breaks, spacing, and indentation.  If the image contains code, place any error messages or annotations directly adjacent to the relevant line of code, as they appear in the image.  Do not add any additional text or commentary beyond what is visible in the image.  If the image contains a combination of different text types, treat them all as a single text block and extract them together."},
                  {"inline_data": {
                    "mime_type": "%s",
                    "data": "%s"
                  }}
                ]
              }]
            }
            """.formatted(mimeType, base64Image);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(GEMINI_API_URL, HttpMethod.POST, requestEntity, String.class);

        return response.getBody();
    }

    private String parseAiResponse(String jsonResponse) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode candidatesNode = rootNode.path("candidates");

            if (candidatesNode.isArray() && !candidatesNode.isEmpty()) {
                JsonNode firstCandidate = candidatesNode.get(0);
                JsonNode contentNode = firstCandidate.path("content").path("parts");

                if (contentNode.isArray() && !contentNode.isEmpty()) {
                    return contentNode.get(0).path("text").asText();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Error extracting text";
    }
}
