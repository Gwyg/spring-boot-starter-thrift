package com.huang.thrift.exception;

/**
 * Thrift Starter 基础异常类。
 * <p>
 * 所有 Thrift Starter 相关异常的基类，提供统一的异常处理机制。
 *
 * @author gwyg
 * @since 1.0.0
 */
public class ThriftException extends RuntimeException {

    /**
     * 构造一个新的 Thrift 异常。
     *
     * @param message 异常描述信息
     */
    public ThriftException(String message) {
        super(message);
    }

    /**
     * 构造一个新的 Thrift 异常。
     *
     * @param message 异常描述信息
     * @param cause 原始异常原因
     */
    public ThriftException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 构造一个新的 Thrift 异常。
     *
     * @param cause 原始异常原因
     */
    public ThriftException(Throwable cause) {
        super(cause);
    }

    /**
     * 构造一个新的 Thrift 异常。
     *
     * @param message 异常描述信息
     * @param cause 原始异常原因
     * @param enableSuppression 是否启用抑制
     * @param writableStackTrace 是否可写堆栈跟踪
     */
    public ThriftException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}