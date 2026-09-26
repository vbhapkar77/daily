package com.vishalbhapkar.daily.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vishalbhapkar.daily.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class UserRepositoryTest {

    @Autowired UserRepository users;

    @Test
    void findsEmailWithoutCaseSensitivity() {
        User saved = users.saveAndFlush(new User("Alice@Example.com", "hashed-password", "Alice"));

        assertThat(users.findByEmail("alice@example.com")).contains(saved);
        assertThat(users.findByEmail("missing@example.com")).isEmpty();
        assertThat(saved.getTimezone()).isEqualTo("UTC");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void rejectsEmailsThatDifferOnlyByCase() {
        users.saveAndFlush(new User("Alice@Example.com", "hashed-password", null));

        assertThatThrownBy(() -> users.saveAndFlush(new User("alice@example.com", "another-hash", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
