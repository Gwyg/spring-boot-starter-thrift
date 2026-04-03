package com.huang.thrift.support;


import com.huang.thrift.annotation.ThriftClient;
import com.huang.thrift.config.NacosConfigProperties;
import com.huang.thrift.config.ThriftClientConfig;
import com.huang.thrift.config.ThriftConstants;
import com.huang.thrift.exception.ClientCreationException;
import com.huang.thrift.factory.ThriftClientProxyFactory;
import com.huang.thrift.factory.ThriftConnectionFactory;
import com.huang.thrift.utils.LoadBalancer;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.thrift.transport.TTransport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thrift 客户端注入器，负责扫描 {@link ThriftClient} 注解并创建相应的代理客户端。
 * <p>
 * 本类实现了 {@link BeanPostProcessor} 接口，在 Spring Bean 初始化后扫描所有字段，
 * 为标记了 {@link ThriftClient} 的字段创建 Thrift 客户端代理并注入。
 * <p>
 * 支持两种模式：
 * 1. Nacos 服务发现模式：通过 Nacos 动态获取服务实例，支持负载均衡
 * 2. 直连模式：直接连接指定的主机和端口，使用连接池管理连接
 * <p>
 * 同时支持 Sentinel 熔断降级功能，可通过配置启用。
 *
 * @author gwyg
 * @since 1.0.0
 */
public class ThriftClientInjector implements BeanPostProcessor {

    /** Nacos 配置属性，条件注入（可选） */
    @Autowired(required = false)
    private NacosConfigProperties nacosConfig;

    /** 负载均衡器，条件注入（可选） */
    @Autowired(required = false)
    private LoadBalancer loadBalancer;

    /** Thrift 客户端配置 */
    @Autowired
    private ThriftClientConfig thriftClientConfig;

    /** 连接池缓存：按服务器标识（host:port）缓存连接池 */
    private final Map<String, GenericObjectPool<TTransport>> connectionPoolMap = new ConcurrentHashMap<>();


    /**
     * 初始化 Thrift 连接池
     *
     * @param host    服务端主机地址
     * @param port    服务端端口
     * @param timeout 连接超时时间（毫秒）
     * @return 配置好的连接池实例
     */
    private GenericObjectPool<TTransport> initConnectionPool(String host, int port, int timeout) {
        // 配置连接池参数
        GenericObjectPoolConfig<TTransport> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(ThriftConstants.CONNECTION_POOL_MAX_TOTAL);
        config.setMaxIdle(ThriftConstants.CONNECTION_POOL_MAX_IDLE);
        config.setMinIdle(ThriftConstants.CONNECTION_POOL_MIN_IDLE);
        config.setTestOnBorrow(true);      // 借用连接时验证有效性
        config.setTestWhileIdle(true);     // 空闲时定期验证
        config.setTimeBetweenEvictionRuns(Duration.ofSeconds(ThriftConstants.CONNECTION_POOL_EVICTION_RUN_INTERVAL));
        config.setMinEvictableIdleTime(Duration.ofSeconds(ThriftConstants.CONNECTION_POOL_MIN_EVICTABLE_IDLE_TIME));

        // 创建连接工厂
        ThriftConnectionFactory factory = new ThriftConnectionFactory(host, port, timeout);

        // 初始化并返回连接池
        return new GenericObjectPool<>(factory, config);
    }

    /**
     * Bean 初始化后处理，扫描 Bean 中的所有字段，为标记了 {@link ThriftClient} 的字段创建并注入代理客户端。
     *
     * @param bean     当前正在初始化的 Bean 实例
     * @param beanName Bean 的名称
     * @return 处理后的 Bean 实例（可能已被修改）
     * @throws RuntimeException 如果创建或注入 Thrift 客户端失败
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, @Nullable String beanName) {
        Field[] fields = bean.getClass().getDeclaredFields();

        for (Field field : fields) {
            ThriftClient annotation = AnnotationUtils.findAnnotation(field, ThriftClient.class);
            if (annotation != null) {
                injectThriftClient(bean, field, annotation);
            }
        }

        return bean;
    }

    /**
     * 为指定字段注入 Thrift 客户端代理
     *
     * @param bean       目标 Bean 实例
     * @param field      需要注入的字段
     * @param annotation 字段上的 {@link ThriftClient} 注解
     * @throws RuntimeException 如果创建或注入客户端失败
     */
    private void injectThriftClient(Object bean, Field field, ThriftClient annotation) {
        try {
            Object client = createThriftClientProxy(field, annotation);
            field.setAccessible(true);
            field.set(bean, client);
        } catch (Exception e) {
            throw new ClientCreationException(ThriftConstants.CLIENT_CREATE_FAILURE_MESSAGE + " for field: " + field.getName(), e);
        }
    }

    /**
     * 根据配置创建 Thrift 客户端代理
     *
     * @param field      客户端字段
     * @param annotation 字段上的 {@link ThriftClient} 注解
     * @return Thrift 客户端代理对象
     * @throws Exception 如果创建代理失败
     */
    private Object createThriftClientProxy(Field field, ThriftClient annotation) throws Exception {
        try {
            // 获取服务名称（优先使用注解配置，默认为字段名）
            String serviceName = getServiceName(field, annotation);

            // 根据是否启用 Nacos 选择不同的创建方式
            if (isNacosEnabled()) {
                return createNacosClientProxy(field, annotation, serviceName);
            } else {
                return createDirectClientProxy(field, annotation, serviceName);
            }
        } catch (Exception e) {
            throw new ClientCreationException(ThriftConstants.CLIENT_CREATE_FAILURE_MESSAGE + " proxy for field: " + field.getName(), e);
        }
    }

    /**
     * 检查是否启用了 Nacos 服务发现
     *
     * @return true 如果 Nacos 已启用且配置有效，否则 false
     */
    private boolean isNacosEnabled() {
        return nacosConfig != null && nacosConfig.isEnabled() && loadBalancer != null;
    }

    /**
     * 获取服务名称
     *
     * @param field      客户端字段
     * @param annotation 字段上的 {@link ThriftClient} 注解
     * @return 服务名称
     */
    private String getServiceName(Field field, ThriftClient annotation) {
        String serviceName = annotation.serviceName();
        return serviceName.isEmpty() ? field.getName() : serviceName;
    }

    /**
     * 创建基于 Nacos 服务发现的客户端代理
     *
     * @param field       客户端字段
     * @param annotation  字段上的 {@link ThriftClient} 注解
     * @param serviceName 服务名称
     * @return Thrift 客户端代理对象
     * @throws Exception 如果创建代理失败
     */
    private Object createNacosClientProxy(Field field, ThriftClient annotation, String serviceName) throws Exception {
        try {
            String nacosName = annotation.nacosName();

            // 根据是否启用熔断创建不同的代理
            if (thriftClientConfig.isCircuitBreakerEnabled()) {
                return ThriftClientProxyFactory.createProxy(
                        null,                       // 不使用连接池
                        loadBalancer,
                        field.getType(),
                        serviceName,
                        nacosName,
                        annotation.fallbackClass());
            } else {
                return ThriftClientProxyFactory.createProxy(
                        loadBalancer,
                        field.getType(),
                        serviceName,
                        nacosName);
            }
        } catch (Exception e) {
            throw new ClientCreationException(ThriftConstants.CLIENT_CREATE_FAILURE_MESSAGE + " for Nacos service: " + serviceName, e);
        }
    }

    /**
     * 创建直连模式的客户端代理
     *
     * @param field       客户端字段
     * @param annotation  字段上的 {@link ThriftClient} 注解
     * @param serviceName 服务名称
     * @return Thrift 客户端代理对象
     * @throws Exception 如果创建代理失败
     */
    private Object createDirectClientProxy(Field field, ThriftClient annotation, String serviceName) throws Exception {
        try {
            // 获取主机和端口（优先使用注解配置，否则使用默认配置）
            String host = getHost(annotation);
            int port = getPort(annotation);

            // 获取或创建连接池
            GenericObjectPool<TTransport> pool = initOrGetPool(host, port);

            // 根据是否启用熔断创建不同的代理
            if (thriftClientConfig.isCircuitBreakerEnabled()) {
                return ThriftClientProxyFactory.createProxy(
                        pool,
                        null,                       // 不使用负载均衡器
                        field.getType(),
                        serviceName,
                        null,                       // 无 Nacos 服务名
                        annotation.fallbackClass());
            } else {
                return ThriftClientProxyFactory.createProxy(
                        pool,
                        field.getType(),
                        serviceName);
            }
        } catch (Exception e) {
            throw new ClientCreationException(ThriftConstants.CLIENT_CREATE_FAILURE_MESSAGE + " for direct service: " + serviceName, e);
        }
    }

    /**
     * 获取服务端主机地址
     *
     * @param annotation 字段上的 {@link ThriftClient} 注解
     * @return 主机地址
     */
    private String getHost(ThriftClient annotation) {
        String host = annotation.host();
        return host.isEmpty() ? thriftClientConfig.getServerHost() : host;
    }

    /**
     * 获取服务端端口
     *
     * @param annotation 字段上的 {@link ThriftClient} 注解
     * @return 端口号
     */
    private int getPort(ThriftClient annotation) {
        int port = annotation.port();
        return port == -1 ? thriftClientConfig.getServerPort() : port;
    }

    /**
     * 初始化或获取现有的连接池
     *
     * @param host 服务端主机地址
     * @param port 服务端端口
     * @return 连接池实例
     */
    private GenericObjectPool<TTransport> initOrGetPool(String host, int port) {
        String key = host + ":" + port;  // 使用 host:port 作为键，避免 HTTP 协议前缀
        return connectionPoolMap.computeIfAbsent(key,
            k -> initConnectionPool(host, port, thriftClientConfig.getTimeout()));
    }

}
