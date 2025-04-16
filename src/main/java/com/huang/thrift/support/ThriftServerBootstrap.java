package com.huang.thrift.support;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.huang.thrift.config.NacosConfigProperties;
import com.huang.thrift.config.ThriftServiceConfig;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.layered.TFramedTransport;
import org.springframework.context.SmartLifecycle;
import java.net.InetAddress;


public class ThriftServerBootstrap implements SmartLifecycle {
    // 服务端配置
    private ThriftServiceConfig config;
    // nacos 配置
    private NacosConfigProperties nacosConfig;

    private volatile boolean running = false;
    private TServer server;
    @Override
    public void start() {
        if(!running){
            try {
                // 创建传输层 从配置中获得端口
                TNonblockingServerSocket serverTransport = new TNonblockingServerSocket(config.getPort());
                // 配置协议工厂
                TProtocolFactory protocolFactory = new TBinaryProtocol.Factory();
                // 配置线程池服务
                TNonblockingServer.Args args = new TNonblockingServer.Args(serverTransport)
                        .transportFactory(new TFramedTransport.Factory())
                        .protocolFactory(protocolFactory)
                        .processor(ThriftServiceProcessor.processor);

                // 创建服务器实例
                server = new TNonblockingServer(args);
                // 启动服务
                new Thread(() -> {
                    System.out.println("Thrift 服务器启动成功");
                    server.serve();
                },"thrift服务端").start();

            }catch (Exception e){
                throw new RuntimeException("Thrift 服务器启动失败", e);
            }
            // 如果启用 Nacos，注册服务
            if (nacosConfig != null && nacosConfig.isEnabled()){
                try {
                    // 注册服务
                    registerToNacos();
                    // 心跳检测
                } catch (Exception e) {
                    throw new RuntimeException("Nacos 注册失败！",e);
                }
            }
        }
    }
    // 注册服务到 Nacos
    private void registerToNacos() throws Exception{
        NamingService namingService = nacosConfig.getNamingService();
        Instance instance = new Instance();
        instance.setIp(InetAddress.getLocalHost().getHostAddress());
        instance.setPort(config.getPort());
        // 设置状态为健康
        instance.setHealthy(true);
        // 临时实例，依赖心跳保持
        instance.setEphemeral(true);
        // 注册服务
        namingService.registerInstance(
                nacosConfig.getServiceName(),
                instance
        );
    }

    @Override
    public void stop() {
        if (running){
            server.stop();
            running = false;
            System.out.println("Thrift 服务器已关闭");
            if(nacosConfig != null && nacosConfig.isEnabled()){
                try {
                    NamingService namingService = NacosFactory.createNamingService(nacosConfig.getNacosAddr());
                    namingService.deregisterInstance(
                            nacosConfig.getServiceName(),
                            nacosConfig.getGroup(),
                            InetAddress.getLocalHost().getHostAddress(),
                            config.getPort()
                    );
                }catch (Exception e){
                    throw new RuntimeException("Nacos 注销失败！",e);
                }
            }
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

    public ThriftServiceConfig getConfig() {
        return config;
    }

    public void setConfig(ThriftServiceConfig config) {
        this.config = config;
    }

    public NacosConfigProperties getNacosConfig() {
        return nacosConfig;
    }

    public void setNacosConfig(NacosConfigProperties nacosConfig) {
        this.nacosConfig = nacosConfig;
    }
}
