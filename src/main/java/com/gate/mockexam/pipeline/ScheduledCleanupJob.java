package com.gate.mockexam.pipeline;

import com.gate.mockexam.pipeline.config.MinioStorageService;
import com.gate.mockexam.pipeline.repository.PaperRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * v2.1 — Scheduled job: deletes raw PDFs from Supabase Storage after
 * successful enrichment + 24-hour safety buffer.
 *
 * PDFs are no longer needed after ingestion (source of truth = DB rows + images).
 * This keeps Supabase storage footprint minimal.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledCleanupJob {

    private final PaperRepository paperRepository;
    private final MinioStorageService storageService;

    @Scheduled(fixedDelayString = "${cleanup.interval-ms:3600000}")
    public void cleanupStagingPdfs() {
        log.debug("[CleanupJob] Running PDF cleanup...");
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        try {
            paperRepository.findByStatusAndUploadedAtBefore("Enriched", cutoff).forEach(paper -> {
                try {
                    String pdfKey = "papers/" + paper.getPaperId() + "/question_paper.pdf";
                    storageService.deleteObject(pdfKey);
                    paper.setStatus("Cleaned");
                    paperRepository.save(paper);
                    log.info("[CleanupJob] Deleted PDF for paperId={}", paper.getPaperId());
                } catch (Exception e) {
                    log.warn("[CleanupJob] Failed for paperId={}: {}", paper.getPaperId(), e.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("[CleanupJob] Cleanup run failed: {}", e.getMessage());
        }
    }
}
