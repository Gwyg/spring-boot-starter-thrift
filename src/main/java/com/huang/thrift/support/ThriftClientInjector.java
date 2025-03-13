package com.huang.thrift.support;

import com.huang.thrift.annotation.ThriftClient;
import com.huang.thrift.factory.ThriftClientProxyFactory;
import com.huang.thrift.factory.ThriftConnectionFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

/**
 * 负责创建并注入 Thrift 客户端实例。
 */
public class ThriftClientInjector implements BeanPostProcessor {

    private GenericObjectPool<TSocket> connectionPool;

    private void initConnectionPool(String host, int port, int timeout){
        // 配置连接池参数
        GenericObjectPoolConfig<TSocket> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(10);
        config.setMaxIdle(5);
        config.setMinIdle(1);
        config.setTestOnBorrow(true); // 借用连接是验证有效性
        // 创建工厂
        ThriftConnectionFactory factory = new ThriftConnectionFactory(host, port, timeout);
        // 初始化连接池
        connectionPool = new GenericObjectPool<>(factory, config);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        // 获取当前 bean 的所有字段信息
        Field[] fields = bean.getClass().getDeclaredFields();
        // 遍历当前 bean 的所有字段，如果有 @ThriftClient 注解，则创建并注入 Thrift 客户端实例
        for (Field field : fields) {
            // 检查是否有 @ThriftClient 注解
            if(field.isAnnotationPresent(ThriftClient.class)){
                // 获取注解信息
                ThriftClient annotation = field.getAnnotation(ThriftClient.class);
                try {
                    String host = annotation.host();
                    int port = annotation.port();
                    int timeout = annotation.timeout();
                    // 初始化连接池
                    if(connectionPool == null) initConnectionPool(host, port, timeout);
                    String serviceName = annotation.serviceName();
                    if(serviceName == null || serviceName.isEmpty()) {
                        serviceName = field.getType().getSimpleName().replace("$Iface", "");
                    }
                    // 利用工厂创建代理对象
                    Object client = ThriftClientProxyFactory.createProxy(connectionPool, field.getType(), annotation.serviceName());
                    field.setAccessible(true);
                    field.set(bean, client);
                }catch (Exception e){
                    // 创建失败
                    throw new RuntimeException("Failed to create Thrift client", e);
                }
            }
        }
        return bean;
    }

}
