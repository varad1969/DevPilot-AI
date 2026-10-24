package DevPilot.Backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the full RAG storage chain using the EXACT indexing path:
 * CodeChunker.chunkFile() -> VectorStore.add() -> similaritySearch().
 * No GitHub OAuth needed - talks directly to the beans.
 */
@Slf4j
@SpringBootTest
class VectorStoreIntegrationTest {

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private DevPilot.Backend.services.indexing.CodeChunker codeChunker;

    @Test
    void embedAndStoreAndRetrieve() {
        String repoId = "test-repo-" + System.currentTimeMillis();

        // Mimic exactly what IndexingService.doIndex() does:
        //   List<Document> chunks = codeChunker.chunkFile(repoId, path, content);
        //   vectorStore.add(new ArrayList<>(batch));
        String content = """
                package com.example;
                public class SecurityConfig {
                    private final TextEncryptor encryptor;
                    public void configure(HttpSecurity http) {
                        http.authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/**").authenticated());
                    }
                }
                """;
        List<Document> docs = codeChunker.chunkFile(repoId, "src/main/java/com/example/SecurityConfig.java", content);
        System.out.println("CHUNKED " + docs.size() + " documents");
        vectorStore.add(docs);

        // Verify rows landed in vector_store
        var filter = new FilterExpressionBuilder()
                .eq("repoId", repoId)
                .build();
        var search = SearchRequest.builder()
                .query("how is authentication configured")
                .topK(5)
                .filterExpression(filter)
                .build();
        List<Document> results = vectorStore.similaritySearch(search);

        System.out.println("RETRIEVED " + results.size() + " documents:");
        results.forEach(d -> System.out.println("  - " + d.getText()));

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getText()).contains("SecurityConfig");
    }
}