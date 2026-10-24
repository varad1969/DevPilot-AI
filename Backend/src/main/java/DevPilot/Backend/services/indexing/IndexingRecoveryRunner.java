package devPilot.backend.services.indexing;

import java.time.Instant;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import devPilot.backend.entity.IndexStatus;
import devPilot.backend.entity.Repository;
import devPilot.backend.repository.RepositoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Recovers repositories that were stuck in INDEXING status due to server restarts or crashes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IndexingRecoveryRunner implements CommandLineRunner {

    private final RepositoryRepository repositoryRepository;

    @Override
    public void run(String... args) {
        try {
            List<Repository> stuckRepos = repositoryRepository.findByIndexStatus(IndexStatus.INDEXING);
            if (!stuckRepos.isEmpty()) {
                log.warn("Found {} repositories stuck in INDEXING status from previous run. Resetting to FAILED.", stuckRepos.size());
                for (Repository repo : stuckRepos) {
                    repo.setIndexStatus(IndexStatus.FAILED);
                    repo.setErrorMessage("Indexing was interrupted by server restart. Click Retry to re-index.");
                    repo.setUpdatedAt(Instant.now());
                    repositoryRepository.save(repo);
                    log.info("Reset stuck repo: {} ({})", repo.getFullName(), repo.getId());
                }
            }
        } catch (Exception ex) {
            log.error("Failed to recover stuck indexing repositories", ex);
        }
    }
}
