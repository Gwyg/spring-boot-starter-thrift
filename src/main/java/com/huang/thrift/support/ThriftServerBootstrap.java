package com.huang.thrift.support;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TServerSocket;
import org.springframework.context.SmartLifecycle;

public class ThriftServerBootstrap implements SmartLifecycle {

    private volatile boolean running = false;
    private TServer server;
    @Override
    public void start() {
        if(!running){
            try {
                // 创建传输层
                TNonblockingServerSocket serverTransport = new TNonblockingServerSocket(9000);
                // 配置协议工厂
                TProtocolFactory protocolFactory = new TBinaryProtocol.Factory();
                // 配置线程池服务
                TNonblockingServer.Args args = new TNonblockingServer.Args(serverTransport)
                        .protocolFactory(protocolFactory)
                        .processor(ThriftServiceProcessor.processor);
                // 创建服务器实例
                server = new TNonblockingServer(args);
                // 启动服务
                new Thread(() -> {
                    System.out.println("Thrift 服务器启动成功");
                    server.serve();
                }).start();

            }catch (Exception e){
                throw new RuntimeException("Thrift 服务器启动失败", e);
            }
        }
    }

    @Override
    public void stop() {
        if (running){
            server.stop();
            running = false;
            System.out.println("Thrift 服务器已关闭");
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}
