package com.vishalbhapkar.daily.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.vishalbhapkar.daily.TestcontainersConfiguration;
import com.vishalbhapkar.daily.user.User;
import com.vishalbhapkar.daily.user.UserRepository;
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
class PasswordResetTokenRepositoryTest {

    @Autowired UserRepository users;
    @Autowired PasswordResetTokenRepository tokens;

    @Test
    void findsOnlyUnusedUnexpiredTokens() {
        User user = users.saveAndFlush(new User("alice@example.com", "hash", null));
        Instant now = Instant.now();
        PasswordResetToken active = tokens.saveAndFlush(
                new PasswordResetToken(user, "active-hash", now.plus(1, ChronoUnit.HOURS)));
        tokens.saveAndFlush(new PasswordResetToken(user, "expired-hash", now.minusSeconds(1)));
        PasswordResetToken used = tokens.saveAndFlush(
                new PasswordResetToken(user, "used-hash", now.plus(1, ChronoUnit.HOURS)));
        used.markUsed();
        tokens.flush();

        assertThat(tokens.findByTokenHash("active-hash")).contains(active);
        assertThat(tokens.findByTokenHashAndUsedAtIsNullAndExpiresAtAfter("active-hash", now))
                .contains(active);
        assertThat(tokens.findByTokenHashAndUsedAtIsNullAndExpiresAtAfter("expired-hash", now))
                .isEmpty();
        assertThat(tokens.findByTokenHashAndUsedAtIsNullAndExpiresAtAfter("used-hash", now))
                .isEmpty();
    }
}
