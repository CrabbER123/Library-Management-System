package com.library.common.exception;

/**
 * 业务错误码。<br>
 * 规则：0 成功；4xxxx 客户端错误；5xxxx 服务端错误。前三位与 HTTP 语义保持一致，便于前端统一处理。
 */
public enum ErrorCode {

    SUCCESS(0, 200, "ok"),

    BAD_REQUEST(40001, 400, "请求参数不合法"),
    UNAUTHORIZED(40100, 401, "未登录或登录已失效"),
    TOKEN_EXPIRED(40101, 401, "登录已过期，请重新登录"),
    INVALID_TOKEN(40102, 401, "无效的凭证"),

    FORBIDDEN(40300, 403, "无权访问该资源"),
    ACCOUNT_DISABLED(40301, 403, "账号已被禁用，请联系管理员"),

    NOT_FOUND(40400, 404, "资源不存在"),

    CONFLICT(40900, 409, "数据冲突，请刷新后重试"),
    DUPLICATE(40901, 409, "数据已存在"),
    LAST_ADMIN(40902, 409, "系统必须保留至少一个管理员账号"),
    CANNOT_OPERATE_SELF(40903, 409, "不能对当前登录账号执行该操作"),

    VALIDATION_ERROR(42200, 422, "业务校验失败"),
    STOCK_NOT_ENOUGH(42201, 422, "图书库存不足"),
    BORROW_LIMIT_EXCEEDED(42202, 422, "已达到该读者的最大借阅数量"),
    READER_INACTIVE(42203, 422, "读者状态不允许借阅"),
    RENEW_LIMIT_EXCEEDED(42204, 422, "已达到最大续借次数"),
    RENEW_OVERDUE(42205, 422, "图书已逾期，不能续借"),
    BOOK_NOT_AVAILABLE(42206, 422, "图书当前不可借阅"),

    INTERNAL_ERROR(50000, 500, "服务器内部错误，请稍后重试");

    private final int code;
    private final int httpStatus;
    private final String message;

    ErrorCode(int code, int httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public String message() {
        return message;
    }
}
