package com.jwd.model;

public enum UserRole {
    GUEST,   // 访客：只读
    MEMBER,  // 会员：读+部分写
    ADMIN    // 管理员：全权限
}