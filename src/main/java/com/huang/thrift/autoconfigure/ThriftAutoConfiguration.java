package com.huang.thrift.autoconfigure;


import com.huang.thrift.config.NacosConfigProperties;
import com.huang.thrift.config.ThriftServiceConfig;
import com.huang.thrift.support.ThriftClientInjector;
import com.huang.thrift.support.ThriftServerBootstrap;
import com.huang.thrift.support.ThriftServiceProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;



@AutoConfiguration
@EnableConfigurationProperties({ThriftServiceConfig.class})
@Import({ThriftServiceProcessor.class, ThriftClientInjector.class})
public class ThriftAutoConfiguration {
    @Bean
    @ConditionalOnBean(ThriftServiceProcessor.class)
    public ThriftServerBootstrap thriftServerBootstrap(ThriftServiceConfig thriftServiceConfig,
                                                       @Autowired(required = false) NacosConfigProperties nacosConfigProperties) {
        ThriftServerBootstrap serverBootstrap = new ThriftServerBootstrap();
        serverBootstrap.setNacosConfig(nacosConfigProperties);
        serverBootstrap.setConfig(thriftServiceConfig);
        return serverBootstrap;
    }
}
