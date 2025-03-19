package com.huang.thrift.test;

import com.huang.thrift.annotation.ThriftClient;
import com.huang.thrift.annotation.ThriftService;
import org.apache.thrift.TException;
import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.transport.layered.TFramedTransport;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.Arrays;

public class Test {

    @ThriftClient
    private static UserService.Iface userService;
    public static void main(String[] args) throws Exception {
        System.out.println(InetAddress.getLocalHost().getHostAddress());
    }

    private static void init() throws Exception {
        TNonblockingServerSocket serverTransport = new TNonblockingServerSocket(9000);
        TProtocolFactory protocolFactory = new TBinaryProtocol.Factory();
        TMultiplexedProcessor processor = new TMultiplexedProcessor();
        processor.registerProcessor("userService", new UserService.Processor<>(new UserServiceImpl()));
        TNonblockingServer.Args args = new TNonblockingServer.Args(serverTransport)
                .transportFactory(new TFramedTransport.Factory())
                .protocolFactory(protocolFactory)
                .processor(processor);
        TNonblockingServer server = new TNonblockingServer(args);
        new Thread(() -> {
            System.out.println("Thrift 服务器启动成功");
            server.serve();
        }).start();
    }

    private static void testclient() throws Exception {
        Field[] fields = Test.class.getDeclaredFields();
        for (Field field : fields) {
            if(field.isAnnotationPresent(ThriftClient.class)){
                ThriftClient annotation = field.getAnnotation(ThriftClient.class);
                System.out.println(field.getType().getEnclosingClass().getName() + "$Client");
                TTransport socket = new TSocket("localhost", 9000);
                socket = new TFramedTransport(socket);
                socket.open();
                TBinaryProtocol protocol = new TBinaryProtocol(socket);
                TMultiplexedProtocol multiplexedProtocol = new TMultiplexedProtocol(protocol, "userService");
                Class<?> aClass = Class.forName(field.getType().getEnclosingClass().getName() + "$Client",
                        true, field.getType().getClassLoader());
                UserService.Iface client = (UserService.Iface) aClass.getDeclaredConstructor(TProtocol.class).newInstance(multiplexedProtocol);
                System.out.println(client.getUser(1));
                Thread.sleep(5000);
                client.getUser(1);
                socket.close();
            }
        }
    }

    private static void testservice() throws TException {
        Object bean = new UserServiceImpl();
        ThriftService annotation = AnnotationUtils.findAnnotation(bean.getClass(), ThriftService.class);
        // 可以优化，防止注解乱加
        if(annotation != null) {
            // 获取服务接口类型
            Class<?> ifaceClass = Arrays.stream(bean.getClass().getInterfaces())
                    .filter(iface -> iface.getName().endsWith("$Iface"))
                    .findFirst()
                    .orElseThrow(() -> new BeanInitializationException("服务必须实现 $Iface 接口: "));
            String name = ifaceClass.getEnclosingClass().getSimpleName();
            name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
            System.out.println(name);
        }
    }
}
