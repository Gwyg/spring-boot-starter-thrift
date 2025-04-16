package com.huang.thrift.support;


import com.huang.thrift.annotation.ThriftClient;
import com.huang.thrift.config.NacosConfigProperties;
import com.huang.thrift.config.ThriftClientConfig;
import com.huang.thrift.factory.ThriftClientProxyFactory;
import com.huang.thrift.factory.ThriftConnectionFactory;
import com.huang.thrift.utils.LoadBalancer;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.thrift.transport.TTransport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 负责创建并注入 Thrift 客户端实例。
 */
public class ThriftClientInjector implements BeanPostProcessor {

    @Autowired(required = false)
    private NacosConfigProperties nacosConfig;
    @Autowired(required = false)
    private LoadBalancer loadBalancer;

    @Autowired
    private ThriftClientConfig thriftClientConfig;

    // 修改后：按服务器标识（host:port）缓存连接池
    private Map<String, GenericObjectPool<TTransport>> connectionPoolMap = new ConcurrentHashMap<>();

    private GenericObjectPool<TTransport> initConnectionPool(String host, int port, int timeout){
        // 配置连接池参数
        GenericObjectPoolConfig<TTransport> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(5);
        config.setMaxIdle(3);
        config.setMinIdle(0);
        config.setTestOnBorrow(true); // 借用连接是验证
        config.setTestWhileIdle(true); // 空闲时定期验证
        config.setTimeBetweenEvictionRuns(Duration.ofSeconds(30000));
        config.setMinEvictableIdleTime(Duration.ofSeconds(60000));
        config.setTestOnBorrow(true); // 借用连接是验证有效性
        // 创建工厂
        ThriftConnectionFactory factory = new ThriftConnectionFactory(host, port, timeout);
        // 初始化连接池
        return new GenericObjectPool<>(factory, config);
    }

    @Override
    // TODO 考虑把创建对象合并，前期为了看着清晰分开了，但是代码现在很冗余
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            // 判断 bean 是否添加了 @ThriftClient 注解
            ThriftClient annotation = AnnotationUtils.findAnnotation(field, ThriftClient.class);
            if (annotation != null) {
                try {
                    // 获取服务名称，默认为字段名
                    String serviceName = annotation.serviceName().isEmpty() ? field.getName() : annotation.serviceName();
                    Object client;
                    // 判断是否使用 Nacos
                    if (nacosConfig != null && nacosConfig.isEnabled()) {
                        // 获取 nacos 的服务名称（需要在注解中配置），否则使用默认值
                        String nacosName = annotation.nacosName();
                        if(!loadBalancer.hasInstances(nacosName)){
                            loadBalancer.updateInstances(nacosName, nacosConfig.getNamingService().getAllInstances(nacosName));
                        }
                        if(thriftClientConfig.isCircuitBreakerEnabled()){
                            client = ThriftClientProxyFactory.createProxy(
                                    null,
                                    loadBalancer,
                                    field.getType(),
                                    serviceName,
                                    nacosName,
                                    annotation.fallbackClass());
                        }else {
                            client = ThriftClientProxyFactory.createProxy(
                                    loadBalancer,
                                    field.getType(),
                                    serviceName,
                                    nacosName);
                        }
                    } else {
                        // 使用默认的 host 和 port
                        String host = annotation.host().isEmpty() ? thriftClientConfig.getServerHost() : annotation.host();
                        int port = annotation.port() == -1 ? thriftClientConfig.getServerPort() : annotation.port();
                        GenericObjectPool<TTransport> pool = initOrGetPool(host, port);
                        if(thriftClientConfig.isCircuitBreakerEnabled()){
                            client = ThriftClientProxyFactory.createProxy(
                                    pool,
                                    null,
                                    field.getType(),
                                    serviceName,
                                    null,
                                    annotation.fallbackClass());
                        }else {
                            client = ThriftClientProxyFactory.createProxy(
                                    pool,
                                    field.getType(),
                                    serviceName);
                        }
                    }
                    field.setAccessible(true);
                    field.set(bean, client);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create Thrift client", e);
                }
            }
        }
        return bean;
    }

    private GenericObjectPool<TTransport> initOrGetPool(String host, int port){
        String key = "http://" + host + ":" + port;
        // 初始化新连接池
        return connectionPoolMap.computeIfAbsent(key, k -> initConnectionPool(host, port, thriftClientConfig.getTimeout()));
    }

}
