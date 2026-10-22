package com.note.common;

public final class UserContext {

    private static final ThreadLocal<AuthUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(AuthUser user) {
        HOLDER.set(user);
    }

    public static AuthUser get() {
        return HOLDER.get();
    }

    public static Long id() {
        AuthUser user = HOLDER.get();
        return user == null ? null : user.getId();
    }

    public static boolean isAdmin() {
        AuthUser user = HOLDER.get();
        return user != null && "ADMIN".equals(user.getRole());
    }

    public static void clear() {
        HOLDER.remove();
    }
}
