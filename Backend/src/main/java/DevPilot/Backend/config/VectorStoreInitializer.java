package devPilot.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class VectorStoreInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Value("${spring.ai.vectorstore.pgvector.dimensions:384}")
    private int configuredDimensions;

    @Override
    public void run(String... args) {
        try {
            // Check if vector_store table exists and what dimension its embedding column has
            String sql = "SELECT atttypmod FROM pg_attribute " +
                    "WHERE attrelid = 'vector_store'::regclass AND attname = 'embedding'";
            Integer currentDim = jdbcTemplate.queryForObject(sql, Integer.class);

            if (currentDim != null && currentDim != configuredDimensions) {
                log.warn("Vector dimension mismatch detected: DB has {}, but configured is {}. Recreating vector_store table.",
                        currentDim, configuredDimensions);
                jdbcTemplate.execute("DROP TABLE IF EXISTS vector_store CASCADE");
            } else {
                log.info("PGVector schema verified with dimension: {}", currentDim != null ? currentDim : configuredDimensions);
            }
        } catch (Exception e) {
            // Table doesn't exist yet, PgVectorStore will initialize it automatically
            log.info("PGVector schema ready for initialization (dimensions: {})", configuredDimensions);
        }
    }
}
