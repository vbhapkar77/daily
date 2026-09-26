package com.vishalbhapkar.daily;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class AuthMigrationTest {

    @Autowired JdbcTemplate jdbc;

    @Test
    void flywayCreatesAuthTablesAndIndexes() {
        List<String> tables = jdbc.queryForList(
                """
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN ('users', 'password_reset_tokens', 'rate_limit_events')
                """,
                String.class);
        assertThat(tables).containsExactlyInAnyOrder("users", "password_reset_tokens", "rate_limit_events");

        String emailType = jdbc.queryForObject(
                "SELECT udt_name FROM information_schema.columns WHERE table_schema = 'public' "
                        + "AND table_name = 'users' AND column_name = 'email'",
                String.class);
        assertThat(emailType).isEqualTo("citext");

        List<String> indexes = jdbc.queryForList(
                """
                SELECT indexname FROM pg_indexes
                WHERE schemaname = 'public'
                  AND tablename IN ('users', 'password_reset_tokens', 'rate_limit_events')
                """,
                String.class);
        assertThat(indexes).contains(
                "users_email_key",
                "idx_users_created_at",
                "idx_prt_user_id",
                "idx_prt_expires_at",
                "idx_rle_bucket_occurred");
    }

    @Test
    void deletingUserCascadesToPasswordResetTokens() {
        Long userId = jdbc.queryForObject(
                "INSERT INTO users (email, password_hash) VALUES ('case@example.com', 'hash') RETURNING id",
                Long.class);
        jdbc.update(
                "INSERT INTO password_reset_tokens (user_id, token_hash, expires_at) "
                        + "VALUES (?, 'token-hash', NOW() + INTERVAL '1 hour')",
                userId);

        jdbc.update("DELETE FROM users WHERE id = ?", userId);

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM password_reset_tokens WHERE user_id = ?", Integer.class, userId);
        assertThat(count).isZero();
    }
}
