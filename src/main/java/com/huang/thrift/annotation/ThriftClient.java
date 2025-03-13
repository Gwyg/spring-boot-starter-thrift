package com.huang.thrift.annotation;

import org.springframework.core.annotation.AliasFor;
import java.lang.annotation.*;


/**
 * 用于标记 Thrift 客户端字段。
 * 被标记的字段会自动注入 Thrift 客户端实例。
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ThriftClient {
    /**
     * Thrift 服务端的主机地址，默认为 localhost。
     */
    String serviceName(); // 服务名称
    int timeout() default 3000;   // 超时时间（毫秒）
    String host() default "localhost";
    /**
     * Thrift 服务端的端口号，默认为 9000。
     */
    int port() default 9000;


}
