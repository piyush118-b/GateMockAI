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

    // TRACK 2+3: new deps introduced in RagIngestionService — mock so context loads without live infra
    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.ai.embedding.EmbeddingModel embeddingModel;

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.gate.mockexam.service.GeminiService geminiService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.ai.vectorstore.VectorStore vectorStore;

    @Test
    public void testIngestAndRetrieveSimilarQuestions() throws Exception {
        List<Document> mockDocs = List.of(
            new Document("Subject: Operating Systems | Topic: Paging\nQuestion: Page replacement policies...", java.util.Map.of("subject", "Operating Systems"))
        );
        org.mockito.Mockito.when(vectorStore.similaritySearch(org.mockito.Mockito.any(org.springframework.ai.vectorstore.SearchRequest.class)))
                .thenReturn(mockDocs);

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
