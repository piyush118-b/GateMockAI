package com.gate.mockexam.pipeline.ingestion;

import com.gate.mockexam.pipeline.config.MinioStorageService;
import com.gate.mockexam.pipeline.domain.Asset;
import com.gate.mockexam.pipeline.domain.GateQuestion;
import com.gate.mockexam.pipeline.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * v2.1 — Diagram Crop Service.
 *
 * Calls the lightweight remote Python crop service (POST /crop-diagram)
 * with the page image bytes + bounding box coordinates returned by Gemini.
 *
 * The Python service crops the region and returns WebP bytes.
 * This service then uploads the WebP to Supabase Storage and saves an Asset record.
 *
 * This is ASYNC — called after DB persistence for questions with hasDiagram=true.
 * Failures do not block the ingestion flow.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DiagramCropService {

    private final MinioStorageService storageService;
    private final AssetRepository assetRepository;

    @Value("${pipeline.crop-service-url:}")
    private String cropServiceUrl;

    private final RestClient restClient = RestClient.builder().build();

    /**
     * Asynchronously crops a diagram from the given page image bytes using
     * the bounding box Gemini returned, then uploads the WebP to Supabase.
     *
     * @param question        the GateQuestion entity (must be persisted)
     * @param pageImageBytes  PNG bytes of the page image from pdf2image
     * @param bbox            normalized bounding box (0–1000 grid)
     */
    @Async
    public void cropAndUploadAsync(GateQuestion question,
                                   byte[] pageImageBytes,
                                   IngestedQuestionResult.DiagramBoundingBox bbox) {
        if (cropServiceUrl == null || cropServiceUrl.isBlank()) {
            log.warn("[DiagramCrop] crop-service-url not configured — skipping diagram crop for {}",
                    question.getQuestionId());
            return;
        }
        try {
            // Build request to Python crop service
            String base64Image = Base64.getEncoder().encodeToString(pageImageBytes);
            Map<String, Object> requestBody = Map.of(
                    "image_base64", base64Image,
                    "bounding_box", Map.of(
                            "y_min", bbox.getYMin(),
                            "x_min", bbox.getXMin(),
                            "y_max", bbox.getYMax(),
                            "x_max", bbox.getXMax()
                    )
            );

            // Call Python crop service
            @SuppressWarnings("unchecked")
            Map<String, String> cropResponse = restClient.post()
                    .uri(cropServiceUrl + "/crop-diagram")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (cropResponse == null || !cropResponse.containsKey("webp_base64")) {
                log.warn("[DiagramCrop] Empty response from crop service for {}", question.getQuestionId());
                return;
            }

            // Upload WebP to Supabase
            byte[] webpBytes = Base64.getDecoder().decode(cropResponse.get("webp_base64"));
            String paperId = question.getPaper().getPaperId();
            String objectKey = MinioStorageService.questionAssetKey(
                    paperId, question.getQuestionId(), "diagram.webp");
            storageService.uploadBytes(objectKey, webpBytes, "image/webp");

            // Save Asset record
            Asset asset = Asset.builder()
                    .assetId(UUID.randomUUID().toString())
                    .question(question)
                    .assetType("Diagram")
                    .bucketName(storageService.getBucket())
                    .objectKey(objectKey)
                    .mimeType("image/webp")
                    .build();
            assetRepository.save(asset);

            log.info("[DiagramCrop] ✅ Uploaded diagram WebP for question {}", question.getQuestionId());

        } catch (Exception e) {
            log.warn("[DiagramCrop] Failed to crop/upload diagram for {}: {}",
                    question.getQuestionId(), e.getMessage());
            // Non-fatal — diagram display is optional
        }
    }
}
