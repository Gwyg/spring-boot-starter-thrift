package com.huang.thrift.autoconfigure;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.huang.thrift.config.NacosConfigProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "spring.thrift.nacos.enabled", havingValue = "true")
public class NacosAutoConfiguration {
    @Bean
    public NacosConfigProperties nacosConfigProperties() {
        NacosConfigProperties nacos = new NacosConfigProperties();
        try {
            nacos.setNamingService(NamingFactory.createNamingService(nacos.getNacosAddr()));
        } catch (NacosException e) {
            System.out.println("Nacos 初始化失败！");
        }
        return nacos;
    }
}
