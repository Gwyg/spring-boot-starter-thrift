package com.huang.thrift.autoconfigure;


import com.huang.thrift.support.ThriftClientInjector;
import com.huang.thrift.support.ThriftServerBootstrap;
import com.huang.thrift.support.ThriftServiceProcessor;
import org.apache.thrift.TMultiplexedProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;


@Configuration
@ConditionalOnClass(TMultiplexedProcessor.class)
@Import({ThriftServiceProcessor.class, ThriftClientInjector.class})
public class ThriftAutoConfiguration {
    @Bean
    @ConditionalOnProperty(name = "thrift.server.enabled", havingValue = "true")
    public ThriftServerBootstrap thriftServerBootstrap() {
        return new ThriftServerBootstrap();
    }

}
