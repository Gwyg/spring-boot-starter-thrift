package com.huang.thrift.support;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.huang.thrift.utils.LoadBalancer;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.layered.TFramedTransport;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class ThriftClientHandler implements InvocationHandler {
    private final LoadBalancer loadBalancer;
    private final GenericObjectPool<TTransport> connectionPool;
    private final Class<?> clientClass;
    private final String serviceName;
    private final String nacosName;

    public ThriftClientHandler(LoadBalancer loadBalancer,
                               GenericObjectPool<TTransport> connectionPool,
                               Class<?> clientClass,
                               String serviceName,
                               String nacosName) {
        this.loadBalancer = loadBalancer;
        this.connectionPool = connectionPool;
        this.clientClass = clientClass;
        this.serviceName = serviceName;
        this.nacosName = nacosName;
    }
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        TTransport socket = null;
        try {
            if(loadBalancer != null){
                // 使用 Nacos 获取服务实例
                Instance instance = loadBalancer.choose(nacosName);
                String host = instance.getIp();
                int port = instance.getPort();
                socket = new TFramedTransport(new TSocket(host, port));
                socket.open();
            }else {
                // 借用连接
                socket = connectionPool.borrowObject();
            }
            TBinaryProtocol protocol = new TBinaryProtocol(socket);
            // 使用 TMultiplexedProtocol
            TMultiplexedProtocol multiplexedProtocol = new TMultiplexedProtocol(protocol, serviceName);
            // 创建 Thrift 客户端实例
            String name = clientClass.getEnclosingClass().getName() + "$Client";
            Class<?> client = Class.forName(name,true,clientClass.getClassLoader());
            Object thriftClient = client.getDeclaredConstructor(TProtocol.class).newInstance(multiplexedProtocol);
            return method.invoke(thriftClient, args);
        }finally {
            if(socket != null){
                if(loadBalancer != null){
                    socket.close();
                }else {
                    connectionPool.returnObject(socket);
                }
            }
        }
    }
}
