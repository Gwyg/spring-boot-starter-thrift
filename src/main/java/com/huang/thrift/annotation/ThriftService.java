package com.huang.thrift.annotation;


import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;


/**
 * 用于标记 Thrift 服务实现类。
 * 被标记的类会被自动注册为 Thrift 服务，并启动一个 Thrift 服务端。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface ThriftService {
    /**
     * Thrift 服务端监听的端口号，默认为 9000。
     */
    int port() default 9000;

    @AliasFor("value")
    String name();
    @AliasFor("name")
    String value();
}
