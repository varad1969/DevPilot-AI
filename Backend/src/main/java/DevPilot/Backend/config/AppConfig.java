package devPilot.backend.config;

import java.io.IOException;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.http.okhttp.OpenAiHttpClientBuilderCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestClient;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

@Configuration
@EnableAsync
@EnableScheduling
public class AppConfig implements org.springframework.beans.factory.InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    @Value("${GROQ_API_KEY:${app.groq.api-key:}}")
    private String groqApiKey;

    @Value("${GROQ_CHAT_MODEL:${spring.ai.openai.chat.options.model:openai/gpt-oss-120b}}")
    private String chatModel;

    @Override
    public void afterPropertiesSet() {
        String key = (groqApiKey != null && !groqApiKey.isBlank()) ? groqApiKey.trim() : System.getenv("GROQ_API_KEY");
        if (key != null && !key.isBlank()) {
            log.info("DevPilot AI Startup: GROQ_API_KEY is configured (starts with: {}...)",
                    key.substring(0, Math.min(8, key.length())));
        } else {
            log.error("DevPilot AI Startup: CRITICAL -> GROQ_API_KEY IS NOT SET IN ENVIRONMENT VARIABLES!");
        }
    }

    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean(name = "indexingExecutor")
    Executor indexingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("index-");
        executor.initialize();
        return executor;
    }

    @Bean
    OpenAiHttpClientBuilderCustomizer openAiHttpClientCustomizer() {
        return builder -> builder.interceptor(chain -> {
            Request request = chain.request();
            String path = request.url().encodedPath();
            Request.Builder reqBuilder = request.newBuilder();

            if (path.contains("chat")) {
                // Force Groq endpoint
                HttpUrl groqUrl = HttpUrl.parse("https://api.groq.com/openai/v1/chat/completions");
                reqBuilder.url(groqUrl);

                // Ensure Groq API Key is used
                String effectiveKey = (groqApiKey != null && !groqApiKey.isBlank()) ? groqApiKey.trim() : "";
                if (effectiveKey.isBlank()) {
                    String envKey = System.getenv("GROQ_API_KEY");
                    if (envKey != null && !envKey.isBlank()) {
                        effectiveKey = envKey.trim();
                    }
                }

                if (!effectiveKey.isBlank()) {
                    reqBuilder.header("Authorization", "Bearer " + effectiveKey);
                } else {
                    log.error("GROQ_API_KEY is NOT set in environment variables! Outgoing chat request has no Groq key!");
                }

                // Ensure model is set to configured chat model
                String targetModel = (chatModel != null && !chatModel.isBlank()) ? chatModel.trim() : "openai/gpt-oss-120b";
                RequestBody body = request.body();
                if (body != null) {
                    try {
                        Buffer buffer = new Buffer();
                        body.writeTo(buffer);
                        String bodyStr = buffer.readUtf8();
                        String updatedBody = bodyStr.replaceAll("\"model\"\\s*:\\s*\"[^\"]+\"",
                                "\"model\":\"" + java.util.regex.Matcher.quoteReplacement(targetModel) + "\"");
                        if (!updatedBody.contains("\"model\"")) {
                            updatedBody = updatedBody.replaceFirst("\\{", "{\"model\":\"" + java.util.regex.Matcher.quoteReplacement(targetModel) + "\",");
                        }
                        MediaType contentType = body.contentType();
                        reqBuilder.method(request.method(), RequestBody.create(updatedBody, contentType));
                    } catch (IOException e) {
                        log.warn("Failed to rewrite model in chat request: {}", e.getMessage());
                    }
                }

                Request finalRequest = reqBuilder.build();
                String authHeader = finalRequest.header("Authorization");
                String maskedKey = (authHeader != null && authHeader.length() > 14)
                        ? authHeader.substring(0, 14) + "..." : authHeader;
                log.info("Dispatching Chat to Groq: model={} url={} (Auth: {})", targetModel, finalRequest.url(), maskedKey);

                Response response = chain.proceed(finalRequest);
                if (!response.isSuccessful()) {
                    ResponseBody resBody = response.body();
                    String errorText = resBody != null ? resBody.string() : "";
                    log.error("Groq chat error: status={} body={}", response.code(), errorText);
                    return response.newBuilder()
                            .body(ResponseBody.create(errorText, resBody != null ? resBody.contentType() : null))
                            .build();
                }
                return response;

            } else if (path.contains("embed")) {
                // Ensure Python Embedder endpoint
                HttpUrl embedUrl = HttpUrl.parse("https://embedder-production-f633.up.railway.app/embeddings");
                reqBuilder.url(embedUrl);
                reqBuilder.header("Authorization", "Bearer dummy-key");

                Request finalRequest = reqBuilder.build();
                log.info("Dispatching Embedding: {} {}", finalRequest.method(), finalRequest.url());

                Response response = chain.proceed(finalRequest);
                if (!response.isSuccessful()) {
                    ResponseBody resBody = response.body();
                    String errorText = resBody != null ? resBody.string() : "";
                    log.error("Embedding service error: status={} body={}", response.code(), errorText);
                    return response.newBuilder()
                            .body(ResponseBody.create(errorText, resBody != null ? resBody.contentType() : null))
                            .build();
                }
                return response;
            }

            return chain.proceed(request);
        });
    }
}
