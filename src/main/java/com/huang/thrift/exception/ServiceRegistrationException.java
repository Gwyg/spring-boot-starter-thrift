package com.huang.thrift.exception;

/**
 * 服务注册异常类。
 * <p>
 * 当 Thrift 服务注册或注销失败时抛出，如 Nacos 注册失败等。
 *
 * @author gwyg
 * @since 1.0.0
 */
public class ServiceRegistrationException extends ThriftException {

    /**
     * 构造一个新的服务注册异常。
     *
     * @param message 异常描述信息
     */
    public ServiceRegistrationException(String message) {
        super(message);
    }

    /**
     * 构造一个新的服务注册异常。
     *
     * @param message 异常描述信息
     * @param cause 原始异常原因
     */
    public ServiceRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 构造一个新的服务注册异常。
     *
     * @param cause 原始异常原因
     */
    public ServiceRegistrationException(Throwable cause) {
        super(cause);
    }
}