package com.huang.thrift.exception;

/**
 * 配置异常类。
 * <p>
 * 当 Thrift 相关配置无效或缺失时抛出。
 *
 * @author gwyg
 * @since 1.0.0
 */
public class ConfigurationException extends ThriftException {

    /**
     * 构造一个新的配置异常。
     *
     * @param message 异常描述信息
     */
    public ConfigurationException(String message) {
        super(message);
    }

    /**
     * 构造一个新的配置异常。
     *
     * @param message 异常描述信息
     * @param cause 原始异常原因
     */
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 构造一个新的配置异常。
     *
     * @param cause 原始异常原因
     */
    public ConfigurationException(Throwable cause) {
        super(cause);
    }
}