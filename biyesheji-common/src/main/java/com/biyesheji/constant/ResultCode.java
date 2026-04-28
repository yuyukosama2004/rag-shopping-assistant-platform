package com.biyesheji.constant;

/**
 * 统一响应状态码
 */
public class ResultCode {

    public static final int SUCCESS = 200;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int TOO_MANY_REQUESTS = 429;
    public static final int SERVER_ERROR = 500;

    // 业务错误码
    public static final int USER_EXISTS = 1001;
    public static final int USER_NOT_FOUND = 1002;
    public static final int PASSWORD_ERROR = 1003;
    public static final int TOKEN_EXPIRED = 1004;
    public static final int STOCK_INSUFFICIENT = 2001;
    public static final int ORDER_DUPLICATE = 2002;
    public static final int ORDER_NOT_FOUND = 2003;
}
