package com.huang.thrift.autoconfigure;


import com.huang.thrift.config.ThriftClientConfig;
import com.huang.thrift.utils.LoadBalancer;
import com.huang.thrift.utils.RandomLoadBalancer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClientAutoConfig {
    @Bean
    public ThriftClientConfig thriftClientConfig() {
        return new ThriftClientConfig();
    }
    @Bean
    @ConditionalOnProperty(name = "spring.thrift.nacos.enabled", havingValue = "true")
    public LoadBalancer loadBalancer() {
        return new RandomLoadBalancer();
    }
}
