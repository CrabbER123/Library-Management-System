package com.library.modules.user.constant;

import java.util.Set;

/**
 * 用户账号状态。
 */
public final class UserStatus {

    public static final String ACTIVE = "ACTIVE";
    public static final String DISABLED = "DISABLED";

    public static final Set<String> ALL = Set.of(ACTIVE, DISABLED);

    private UserStatus() {
    }
}
