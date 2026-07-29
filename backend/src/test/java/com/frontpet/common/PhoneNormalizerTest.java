package com.frontpet.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneNormalizerTest {

    @Test
    void normalizesLocalFormatWithDdd() {
        assertThat(PhoneNormalizer.normalizeBr("(51) 99999-8888")).isEqualTo("5551999998888");
    }

    @Test
    void isIdempotentWhenAlreadyNormalized() {
        assertThat(PhoneNormalizer.normalizeBr("5551999998888")).isEqualTo("5551999998888");
    }

    @Test
    void rejectsTooFewDigits() {
        assertThatThrownBy(() -> PhoneNormalizer.normalizeBr("12345"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> PhoneNormalizer.normalizeBr(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
