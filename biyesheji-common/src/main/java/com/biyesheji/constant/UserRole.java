package com.biyesheji.constant;

/**
 * 用户角色。数值与既有数据库中的管理员角色兼容：1 现在表示单店店主。
 */
public final class UserRole {

    public static final int CUSTOMER = 0;
    public static final int OWNER = 1;
    public static final int STAFF = 2;

    private UserRole() {
    }
}
