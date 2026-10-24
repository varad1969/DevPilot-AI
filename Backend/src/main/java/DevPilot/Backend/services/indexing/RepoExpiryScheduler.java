package devPilot.backend.services.indexing;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import devPilot.backend.entity.IndexStatus;
import devPilot.backend.entity.Repository;
import devPilot.backend.repository.RepositoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Daily scheduled job that automatically expires repositories that were indexed
 * more than {@code app.indexing.expiry-days} days ago (default: 5 days).
 *
 * <p>Expired repos have their vector embeddings deleted from the vector store
 * and their status reset to {@link IndexStatus#EXPIRED}. Users can re-index
 * them at any time from the dashboard, keeping the vector store lean.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RepoExpiryScheduler {

    private final RepositoryRepository repositoryRepository;
    private final IndexingService indexingService;

    /** How many days a repo stays indexed before being auto-expired (default 5). */
    @Value("${app.indexing.expiry-days:5}")
    private int expiryDays;

    /**
     * Runs every day at 02:00 AM server time.
     * Finds all READY repos whose {@code indexedAt} is older than the TTL and
     * expires them one by one.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void expireStaleRepos() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(expiryDays));
        List<Repository> stale = repositoryRepository
                .findByIndexStatusAndIndexedAtBefore(IndexStatus.READY, cutoff);

        if (stale.isEmpty()) {
            log.debug("Repo expiry job: no stale repositories found (cutoff={})", cutoff);
            return;
        }

        log.info("Repo expiry job: found {} repositories indexed before {} – expiring now",
                stale.size(), cutoff);

        int success = 0;
        int failed = 0;
        for (Repository repo : stale) {
            try {
                indexingService.expireRepo(repo.getId());
                success++;
            } catch (Exception ex) {
                log.error("Failed to expire repo {} ({}): {}",
                        repo.getFullName(), repo.getId(), ex.getMessage(), ex);
                failed++;
            }
        }

        log.info("Repo expiry job complete: {} expired, {} failed", success, failed);
    }
}
