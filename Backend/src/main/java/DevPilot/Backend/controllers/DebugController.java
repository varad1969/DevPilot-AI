package devPilot.backend.controllers;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private final ChatModel chatModel;
    private final EmbeddingModel embeddingModel;

    @Value("${GROQ_API_KEY:${app.groq.api-key:}}")
    private String groqApiKey;

    @Value("${OPENAI_API_KEY:}")
    private String openAiApiKey;

    @Value("${GROQ_CHAT_MODEL:${spring.ai.openai.chat.options.model:openai/gpt-oss-120b}}")
    private String configuredChatModel;

    public DebugController(ChatModel chatModel, EmbeddingModel embeddingModel) {
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getDebugStatus() {
        Map<String, Object> map = new HashMap<>();

        String gKey = (groqApiKey != null && !groqApiKey.isBlank()) ? groqApiKey.trim() : System.getenv("GROQ_API_KEY");
        map.put("groqApiKeyConfigured", gKey != null && !gKey.isBlank());
        map.put("groqApiKeyPrefix", (gKey != null && gKey.length() > 6) ? gKey.substring(0, 7) : "none");
        map.put("openaiApiKeyConfigured", openAiApiKey != null && !openAiApiKey.isBlank());
        map.put("configuredChatModel", configuredChatModel);

        // 1. Test embedding with python embedder
        try {
            float[] embed = embeddingModel.embed("test query");
            map.put("embeddingSuccess", true);
            map.put("embeddingDims", embed != null ? embed.length : 0);
        } catch (Exception e) {
            map.put("embeddingSuccess", false);
            map.put("embeddingError", e.getClass().getName() + ": " + e.getMessage());
        }

        // 2. Direct HTTP chat completions test for candidate models
        Map<String, Object> candidateTests = new HashMap<>();
        if (gKey != null && !gKey.isBlank()) {
            String[] candidates = new String[] { "openai/gpt-oss-120b", "openai/gpt-oss-20b", "qwen/qwen3.8-27b" };
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            for (String modelName : candidates) {
                try {
                    String payload = "{\"model\":\"" + modelName + "\",\"messages\":[{\"role\":\"user\",\"content\":\"Hi\"}],\"max_tokens\":10}";
                    java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                            .uri(java.net.URI.create("https://api.groq.com/openai/v1/chat/completions"))
                            .header("Authorization", "Bearer " + gKey)
                            .header("Content-Type", "application/json")
                            .POST(java.net.http.HttpRequest.BodyPublishers.ofString(payload))
                            .build();
                    java.net.http.HttpResponse<String> resp = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
                    candidateTests.put(modelName, Map.of("status", resp.statusCode(), "body", resp.body()));
                } catch (Exception e) {
                    candidateTests.put(modelName, Map.of("error", e.getMessage()));
                }
            }
        }
        map.put("candidateTests", candidateTests);

        // 3. Test chat completion with configured ChatModel bean
        try {
            String reply = chatModel.call("Say hello in one word");
            map.put("chatSuccess", true);
            map.put("chatReply", reply);
        } catch (Exception e) {
            map.put("chatSuccess", false);
            map.put("chatError", e.getClass().getName() + ": " + e.getMessage());
        }

        return ResponseEntity.ok(map);
    }
}
