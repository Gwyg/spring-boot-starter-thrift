package com.huang.thrift.factory;

import com.huang.thrift.autoconfigure.ThriftAutoConfiguration;
import com.huang.thrift.support.ThriftClientHandler;
import com.huang.thrift.utils.LoadBalancer;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.thrift.transport.TTransport;

import java.lang.reflect.Proxy;

public class ThriftClientProxyFactory {
    public static Object createProxy(GenericObjectPool<TTransport> connectionPool, Class<?> clientClass, String serviceName){
        return Proxy.newProxyInstance(
                ThriftAutoConfiguration.class.getClassLoader(),
                new Class[]{clientClass},
                new ThriftClientHandler(null,connectionPool,clientClass,serviceName)
        );
    }
    public static Object createProxy(LoadBalancer loadBalancer, Class<?> clientClass, String serviceName){
        return Proxy.newProxyInstance(
                ThriftAutoConfiguration.class.getClassLoader(),
                new Class[]{clientClass},
                new ThriftClientHandler(loadBalancer,null,clientClass,serviceName)
        );
    }

}
