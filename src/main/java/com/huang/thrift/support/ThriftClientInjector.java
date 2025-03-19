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
        // TODO 客户端 Nacos 的实现
        // 配置连接池参数
        GenericObjectPoolConfig<TTransport> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(5);
        config.setMaxIdle(3);
        config.setMinIdle(1);
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

    /*@Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        // 获取当前 bean 的所有字段信息
        Field[] fields = bean.getClass().getDeclaredFields();
        // 遍历当前 bean 的所有字段，如果有 @ThriftClient 注解，则创建并注入 Thrift 客户端实例
        for (Field field : fields) {
            // 检查是否有 @ThriftClient 注解
            ThriftClient annotation = AnnotationUtils.findAnnotation(field, ThriftClient.class);
            if(annotation != null){
                try {
                    // 获取配的地址
                    String host = annotation.host().isEmpty() ? thriftClientConfig.getServer_host() : annotation.host();
                    int port = annotation.port() == -1 ? thriftClientConfig.getServer_port() : annotation.port();
                    // 获取化连接池
                    GenericObjectPool<TTransport> pool = initOrGetPool(host, port);
                    String serviceName = annotation.serviceName().isEmpty() ? field.getName() : annotation.serviceName();
                    // 利用工厂创建代理对象
                    Object client = ThriftClientProxyFactory.createProxy(pool, field.getType(), serviceName);
                    field.setAccessible(true);
                    field.set(bean, client);
                }catch (Exception e){
                    // 创建失败
                    throw new RuntimeException("Failed to create Thrift client", e);
                }
            }
        }
        return bean;
    }*/
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            ThriftClient annotation = AnnotationUtils.findAnnotation(field, ThriftClient.class);
            if (annotation != null) {
                try {
                    String serviceName = annotation.serviceName().isEmpty() ? field.getName() : annotation.serviceName();
                    Object client;
                    if (nacosConfig != null && nacosConfig.isEnabled()) {
                        // 使用 Nacos 获取服务实例
                        String nacosName = annotation.nacosName();
                        if(!loadBalancer.hasInstances(nacosName)){
                            loadBalancer.updateInstances(nacosName, nacosConfig.getNamingService().getAllInstances(nacosName));
                        }
                        client = ThriftClientProxyFactory.createProxy(loadBalancer, field.getType(), serviceName);
                    } else {
                        // 使用默认的 host 和 port
                        String host = annotation.host().isEmpty() ? thriftClientConfig.getServer_host() : annotation.host();
                        int port = annotation.port() == -1 ? thriftClientConfig.getServer_port() : annotation.port();
                        GenericObjectPool<TTransport> pool = initOrGetPool(host, port);
                        client = ThriftClientProxyFactory.createProxy(pool, field.getType(), serviceName);
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
