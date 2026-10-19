package com.computeraccessories.pos;

record User(long id, String username, String displayName, String role) {
    boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
