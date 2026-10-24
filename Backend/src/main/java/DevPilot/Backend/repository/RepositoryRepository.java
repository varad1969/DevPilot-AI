package devPilot.backend.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import devPilot.backend.entity.IndexStatus;
import devPilot.backend.entity.Repository;

public interface RepositoryRepository extends JpaRepository<Repository, UUID> {
    List<Repository> findByUserIdOrderByFullNameAsc(UUID userId);

    Optional<Repository> findByIdAndUserId(UUID id, UUID userId);

    Optional<Repository> findByUserIdAndGithubRepoId(UUID userId, Long githubRepoId);

    @Query("SELECT r FROM Repository r WHERE r.indexStatus = :status AND r.indexedAt < :cutoff")
    List<Repository> findByIndexStatusAndIndexedAtBefore(
            @Param("status") IndexStatus status,
            @Param("cutoff") Instant cutoff);

    List<Repository> findByIndexStatus(IndexStatus status);
}

