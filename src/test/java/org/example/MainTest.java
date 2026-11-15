package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @Test
    void parse_positive_int_accepts_only_whole_numbers_above_zero() {
        assertEquals(8, Main.parse_positive_int("8"));
        assertEquals(8, Main.parse_positive_int(" 8 "));
        assertNull(Main.parse_positive_int("0"));
        assertNull(Main.parse_positive_int("-3"));
        assertNull(Main.parse_positive_int("abc"));
        assertNull(Main.parse_positive_int("2.5"));
        assertNull(Main.parse_positive_int(""));
    }
}
