package com.smartcollab.local.controller;

import com.smartcollab.local.service.AIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;

/**
 * AI 기능(요약, 번역)을 위한 Mock Controller.
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;

    @PostMapping("/summarize/{fileId}")
    public ResponseEntity<String> summarizeFile(@PathVariable Long fileId,
                                                @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        String summary = aiService.summarize(fileId, userDetails.getUsername());
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/translate/{fileId}")
    public ResponseEntity<String> translateFile(@PathVariable Long fileId,
                                                @RequestParam(defaultValue = "en") String targetLanguage,
                                                @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        String translatedText = aiService.translate(fileId, targetLanguage, userDetails.getUsername());
        return ResponseEntity.ok(translatedText);
    }
}