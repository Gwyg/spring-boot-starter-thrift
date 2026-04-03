package com.huang.thrift.exception;

/**
 * 客户端创建异常类。
 * <p>
 * 当 Thrift 客户端代理创建或注入失败时抛出。
 *
 * @author gwyg
 * @since 1.0.0
 */
public class ClientCreationException extends ThriftException {

    /**
     * 构造一个新的客户端创建异常。
     *
     * @param message 异常描述信息
     */
    public ClientCreationException(String message) {
        super(message);
    }

    /**
     * 构造一个新的客户端创建异常。
     *
     * @param message 异常描述信息
     * @param cause 原始异常原因
     */
    public ClientCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 构造一个新的客户端创建异常。
     *
     * @param cause 原始异常原因
     */
    public ClientCreationException(Throwable cause) {
        super(cause);
    }
}