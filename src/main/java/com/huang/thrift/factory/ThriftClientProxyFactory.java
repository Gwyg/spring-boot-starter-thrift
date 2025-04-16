package com.huang.thrift.factory;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.huang.thrift.autoconfigure.ThriftAutoConfiguration;
import com.huang.thrift.support.ThriftClientHandler;
import com.huang.thrift.utils.LoadBalancer;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.thrift.transport.TTransport;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

public class ThriftClientProxyFactory {
    // 创建代理对象
    public static Object createProxy(GenericObjectPool<TTransport> connectionPool,
                                     Class<?> clientClass,
                                     String serviceName) {
        return Proxy.newProxyInstance(
                ThriftAutoConfiguration.class.getClassLoader(),
                new Class[]{clientClass},
                new ThriftClientHandler(null, connectionPool, clientClass, serviceName, null)
        );
    }

    // 创建开启了 Nacos 服务的代理对象
    public static Object createProxy(LoadBalancer loadBalancer,
                                     Class<?> clientClass,
                                     String serviceName,
                                     String nacosName) {
        return Proxy.newProxyInstance(
                ThriftAutoConfiguration.class.getClassLoader(),
                new Class[]{clientClass},
                new ThriftClientHandler(loadBalancer, null, clientClass, serviceName, nacosName)
        );
    }

    // 创建具有熔断功能的代理对象
    public static Object createProxy(GenericObjectPool<TTransport> connectionPool,
                                     LoadBalancer loadBalancer,
                                     Class<?> clientClass,
                                     String serviceName,
                                     String nacosName,
                                     Class<?> fallbackClass) {
        ThriftClientHandler handler = new ThriftClientHandler(loadBalancer, connectionPool, clientClass, serviceName, nacosName);
        Object fallback = createFallback(fallbackClass);
        return Proxy.newProxyInstance(
                ThriftAutoConfiguration.class.getClassLoader(),
                new Class[]{clientClass},
                (proxy, method, args) -> {
                    String resouce = clientClass.getName() + "#" + method.getName();
                    Entry entry = null;
                    try {
                        entry = SphU.entry(resouce);
                        return handler.invoke(proxy, method, args);
                    }catch (BlockException e){
                        if(fallback != null){
                            // 出异常就执行Fallback
                            return method.invoke(fallback, args);
                        }
                        throw new RuntimeException("没有定义服务降级", e);
                    } catch (Throwable e) {
                        Tracer.trace(e);
                        throw e;
                    }
                    finally {
                        if(entry != null) entry.exit();
                    }
                }
        );
    }

    // 创建 Fallback 对象
    private static Object createFallback(Class<?> fallbackClass) {
        try {
            return fallbackClass == void.class ? null : fallbackClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("不能创建 Fallback 对象", e);
        }
    }

}
