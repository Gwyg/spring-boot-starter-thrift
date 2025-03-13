package com.huang.thrift.factory;

import com.huang.thrift.support.ThriftClientHandler;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.thrift.transport.TSocket;

import java.lang.reflect.Proxy;

public class ThriftClientProxyFactory {
    public static Object createProxy(GenericObjectPool<TSocket> connectionPool, Class<?> clientClass, String serviceName){
        return Proxy.newProxyInstance(
                clientClass.getClassLoader(),
                clientClass.getInterfaces(),
                new ThriftClientHandler(connectionPool,clientClass,serviceName)
        );
    }
}
