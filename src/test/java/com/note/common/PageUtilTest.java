package com.note.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class PageUtilTest {

    @Test
    void rejectsInvalidPage() {
        assertThrows(BusinessException.class, () -> PageUtil.check(0, 10));
        assertThrows(BusinessException.class, () -> PageUtil.check(1, 101));
    }

    @Test
    void acceptsValidPage() {
        PageUtil.check(1, 10);
    }
}
