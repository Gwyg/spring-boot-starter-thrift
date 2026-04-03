package com.huang.thrift.autoconfigure;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.huang.thrift.config.NacosConfigProperties;
import com.huang.thrift.config.ThriftConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Nacos 自动配置类。
 * <p>
 * 当配置项 {@code spring.thrift.nacos.enabled} 为 {@code true} 时自动启用。
 * 负责创建和配置 Nacos 命名服务实例。
 *
 * @author gwyg
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(name = "spring.thrift.nacos.enabled", havingValue = "true")
public class NacosAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(NacosAutoConfiguration.class);

    /**
     * 创建 Nacos 配置属性 Bean。
     * <p>
     * 初始化 Nacos 命名服务并设置到配置属性中。
     * 如果初始化失败，会记录错误日志，但不会阻止 Bean 创建（允许降级使用直连模式）。
     *
     * @return Nacos 配置属性实例
     */
    @Bean
    public NacosConfigProperties nacosConfigProperties() {
        NacosConfigProperties nacos = new NacosConfigProperties();
        String nacosAddr = nacos.getNacosAddr();

        LOGGER.info("正在初始化 Nacos 命名服务，地址：{}", nacosAddr);

        try {
            nacos.setNamingService(NamingFactory.createNamingService(nacosAddr));
            LOGGER.info("Nacos 命名服务初始化成功，地址：{}", nacosAddr);
        } catch (NacosException e) {
            LOGGER.error(ThriftConstants.NACOS_INIT_FAILURE_MESSAGE, e);
            // 不抛出异常，允许降级到直连模式
            // 后续使用 Nacos 时会通过 null 检查避免 NPE
        }

        return nacos;
    }
}
