package devPilot.backend.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
@Slf4j
public class DataSourceConfig {

    @Value("${DATABASE_URL:}")
    private String databaseUrl;

    @Value("${DB_URL:}")
    private String dbUrl;

    @Value("${PGHOST:}")
    private String pghost;

    @Value("${PGPORT:5432}")
    private String pgport;

    @Value("${PGDATABASE:railway}")
    private String pgdatabase;

    @Value("${DB_USERNAME:${PGUSER:${POSTGRES_USER:postgres}}}")
    private String defaultUsername;

    @Value("${DB_PASSWORD:${PGPASSWORD:${POSTGRES_PASSWORD:postgres}}}")
    private String defaultPassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.postgresql.Driver");

        String selectedUrl = "";
        String username = defaultUsername;
        String password = defaultPassword;

        // Priority 1: Check DATABASE_URL (Railway standard)
        if (StringUtils.hasText(databaseUrl)) {
            selectedUrl = databaseUrl.trim();
        } else if (StringUtils.hasText(dbUrl)) {
            selectedUrl = dbUrl.trim();
        } else if (StringUtils.hasText(pghost) && !pghost.equals("localhost")) {
            selectedUrl = "jdbc:postgresql://" + pghost + ":" + pgport + "/" + pgdatabase;
        } else {
            selectedUrl = "jdbc:postgresql://localhost:5433/devpilot";
        }

        // Parse postgresql:// or postgres:// URI if provided by Railway
        if (selectedUrl.startsWith("postgres://") || selectedUrl.startsWith("postgresql://")) {
            try {
                String normalized = selectedUrl.startsWith("postgres://")
                        ? "http://" + selectedUrl.substring("postgres://".length())
                        : "http://" + selectedUrl.substring("postgresql://".length());
                URI uri = new URI(normalized);

                if (StringUtils.hasText(uri.getUserInfo())) {
                    String[] creds = uri.getUserInfo().split(":", 2);
                    username = creds[0];
                    if (creds.length > 1) {
                        password = creds[1];
                    }
                }

                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                String path = uri.getPath();
                if (!StringUtils.hasText(path) || path.equals("/")) {
                    path = "/" + pgdatabase;
                }

                selectedUrl = "jdbc:postgresql://" + host + ":" + port + path;
            } catch (Exception e) {
                log.warn("Failed to parse DATABASE_URL as URI, prepending jdbc: prefix: {}", e.getMessage());
                if (!selectedUrl.startsWith("jdbc:")) {
                    selectedUrl = "jdbc:" + selectedUrl;
                }
            }
        } else if (!selectedUrl.startsWith("jdbc:")) {
            selectedUrl = "jdbc:" + selectedUrl;
        }

        log.info("Connecting to database at: {}", selectedUrl.replaceAll(":([^/@]+)@", ":****@"));
        config.setJdbcUrl(selectedUrl);
        config.setUsername(username);
        config.setPassword(password);

        config.setConnectionTimeout(30000);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);

        return new HikariDataSource(config);
    }
}
