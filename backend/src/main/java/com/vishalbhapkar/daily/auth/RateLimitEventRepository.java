package com.vishalbhapkar.daily.auth;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RateLimitEventRepository extends JpaRepository<RateLimitEvent, Long> {

    long countByBucketKeyAndOccurredAtAfter(String bucketKey, Instant since);

    @Query("select min(e.occurredAt) from RateLimitEvent e "
            + "where e.bucketKey = :bucketKey and e.occurredAt > :since")
    Optional<Instant> findEarliestInWindow(
            @Param("bucketKey") String bucketKey, @Param("since") Instant since);
}
