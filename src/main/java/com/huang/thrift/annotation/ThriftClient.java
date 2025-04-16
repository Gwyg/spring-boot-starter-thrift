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

    @AliasFor("serviceName")
    String value() default "";
    @AliasFor("value")
    String serviceName() default ""; // 服务名称
    // 服务地址 默认 localhost
    String host() default "";
    /**
     * Thrift 服务端的端口号，默认为 9000。
     */
    int port() default -1;
    // 注册在 nacos 的服务名
    String nacosName() default "thrift-service";

    // 降级类（需实现同一接口）
    Class<?> fallbackClass() default void.class;
}
