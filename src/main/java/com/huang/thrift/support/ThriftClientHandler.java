package com.huang.thrift.support;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class ThriftClientHandler implements InvocationHandler {
    private final GenericObjectPool<TSocket> connectionPool;
    private final Class<?> clientClass;
    private final String serviceName;

    public ThriftClientHandler(GenericObjectPool<TSocket> connectionPool, Class<?> clientClass, String serviceName) {
        this.connectionPool = connectionPool;
        this.clientClass = clientClass;
        this.serviceName = serviceName;
    }
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        TSocket socket = null;
        try {
            // 借用连接
            socket = connectionPool.borrowObject();
            TProtocol protocol = new TBinaryProtocol(socket);
            // 使用 TMultiplexedProtocol
            TMultiplexedProtocol multiplexedProtocol = new TMultiplexedProtocol(protocol, serviceName);
            // 创建 Thrift 客户端实例
            Class<?> client = Class.forName(clientClass.getName().replace("$Iface", "$Client"));
            Object thriftClient = client.getConstructor(TProtocol.class).newInstance(multiplexedProtocol);
            return method.invoke(thriftClient, args);
        }finally {
            // 归还连接
            if(socket != null) connectionPool.returnObject(socket);
        }
    }
}
