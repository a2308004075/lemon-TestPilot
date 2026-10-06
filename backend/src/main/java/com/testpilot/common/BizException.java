package com.testpilot.common;

/**
 * 业务异常：抛出后由全局异常处理器转为统一响应。
 */
public class BizException extends RuntimeException {

    public BizException(String message) {
        super(message);
    }
}
