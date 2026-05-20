package com.gate.mockexam.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/admin/upload")
@Slf4j
public class MediaUploadController {

    @Value("${gate.uploads.dir:./uploads/questions}")
    private String uploadsDir;

    /**
     * POST /admin/upload/image
     * Accepts: multipart/form-data with field "file"
     * Returns: { "url": "/uploads/questions/{uuid}.ext" }
     */
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/"))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only image files are allowed"));
        }

        try {
            // Determine extension from original filename
            String originalFilename = file.getOriginalFilename();
            String extension = "png";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
            }

            // Sanitize: allow only safe extensions
            if (!extension.matches("png|jpg|jpeg|gif|svg|webp")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Unsupported file type: " + extension));
            }

            String filename = UUID.randomUUID() + "." + extension;
            Path uploadPath = Paths.get(uploadsDir);
            Files.createDirectories(uploadPath);

            Path destination = uploadPath.resolve(filename);
            file.transferTo(destination.toFile());

            String publicUrl = "/uploads/questions/" + filename;
            log.info("Uploaded image: {} → {}", originalFilename, publicUrl);
            return ResponseEntity.ok(Map.of("url", publicUrl));

        } catch (IOException e) {
            log.error("Failed to save uploaded image", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }
}
