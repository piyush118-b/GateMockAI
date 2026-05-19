package com.gate.mockexam.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ai.document.Document;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class RagIngestionServiceTest {

    @Autowired
    private RagIngestionService ragIngestionService;

    @Test
    public void testIngestAndRetrieveSimilarQuestions() throws Exception {
        // Ensure that at least some questions are ingested into the store
        int count = ragIngestionService.getVectorCount();
        if (count == 0) {
            count = ragIngestionService.ingestSeedQuestions();
        }
        assertThat(count).isGreaterThan(0);

        // Perform similarity search for OS Virtual Memory
        List<Document> matches = ragIngestionService.retrieveSimilarQuestions("Virtual memory and page replacement policies", 3);
        
        assertThat(matches).isNotEmpty();
        
        // Print the matches for diagnostic visibility
        System.out.println("--- similarity search matches ---");
        for (Document doc : matches) {
            System.out.println("Score/Content: " + doc.getText());
            System.out.println("Metadata: " + doc.getMetadata());
        }

        // Verify the top match relates to Operating Systems or Paging Management
        String firstMatchContent = matches.get(0).getText().toLowerCase();
        assertThat(firstMatchContent).containsAnyOf("memory", "page", "paging", "fault");
    }
}
