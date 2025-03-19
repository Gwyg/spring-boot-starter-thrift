package com.huang.thrift.config;

import com.alibaba.nacos.api.naming.NamingService;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.thrift.nacos")
public class NacosConfigProperties {
    // 是否开启 nacos 服务
    private boolean enabled = false;
    // nacos 服务地址 默认为 127.0.0.1:8848
    private String nacosAddr = "127.0.0.1:8848";
    // nacos 命名空间
    private String namespace;
    // nacos 配置组
    private String group;
    // nacos 服务名称 默认为 spring.application.name 的值,没设置就为 thrift-server
    private String serviceName = "thrift-server";
    private NamingService namingService;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getNacosAddr() {
        return nacosAddr;
    }

    public void setNacosAddr(String nacosAddr) {
        this.nacosAddr = nacosAddr;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public NamingService getNamingService() {
        return namingService;
    }

    public void setNamingService(NamingService namingService) {
        this.namingService = namingService;
    }
}
