package com.vishalbhapkar.daily.lib.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void hashesWithBcryptCostTwelveAndVerifiesPassword() {
        String first = hasher.hash("correct horse battery staple");
        String second = hasher.hash("correct horse battery staple");

        assertThat(first).matches("\\$2[aby]\\$12\\$.{53}");
        assertThat(second).isNotEqualTo(first);
        assertThat(hasher.verify("correct horse battery staple", first)).isTrue();
        assertThat(hasher.verify("wrong password", first)).isFalse();
    }
}
