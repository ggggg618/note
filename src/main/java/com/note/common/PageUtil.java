package com.note.common;

public final class PageUtil {

    private static final long MAX_SIZE = 100;

    private PageUtil() {
    }

    public static void check(long page, long size) {
        if (page < 1) {
            throw new BusinessException("页码必须大于0");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new BusinessException("每页数量需在1到100之间");
        }
    }
}
