package com.vishalbhapkar.daily.lib.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class SecureTokensTest {

    private final SecureTokens tokens = new SecureTokens();

    @Test
    void generatesDistinctUrlSafeTokensFromThirtyTwoRandomBytes() {
        String first = tokens.generate();
        String second = tokens.generate();

        assertThat(first).matches("[A-Za-z0-9_-]{43}");
        assertThat(Base64.getUrlDecoder().decode(first)).hasSize(32);
        assertThat(second).isNotEqualTo(first);
    }
}
