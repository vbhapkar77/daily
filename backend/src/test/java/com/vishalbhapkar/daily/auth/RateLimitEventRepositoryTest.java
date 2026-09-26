package com.vishalbhapkar.daily.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.vishalbhapkar.daily.TestcontainersConfiguration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class RateLimitEventRepositoryTest {

    @Autowired RateLimitEventRepository events;

    @Test
    void countsEventsByBucketAndWindowAndFindsEarliest() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        Instant since = now.minusSeconds(900);
        Instant first = now.minusSeconds(600);
        events.save(new RateLimitEvent("login:alice", now.minusSeconds(1000)));
        events.save(new RateLimitEvent("login:alice", first));
        events.save(new RateLimitEvent("login:alice", now.minusSeconds(60)));
        events.save(new RateLimitEvent("login:bob", now.minusSeconds(30)));
        events.flush();

        assertThat(events.countByBucketKeyAndOccurredAtAfter("login:alice", since)).isEqualTo(2);
        assertThat(events.findEarliestInWindow("login:alice", since)).contains(first);
        assertThat(events.findEarliestInWindow("login:unknown", since)).isEmpty();
    }
}
