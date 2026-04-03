package com.huang.thrift.support;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.huang.thrift.config.NacosConfigProperties;
import com.huang.thrift.config.ThriftConstants;
import com.huang.thrift.config.ThriftServiceConfig;
import com.huang.thrift.exception.ServiceRegistrationException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.layered.TFramedTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import java.net.InetAddress;


/**
 * Thrift 服务端启动器，负责启动和管理 Thrift 服务器。
 * <p>
 * 实现 {@link SmartLifecycle} 接口，与 Spring 生命周期集成。
 * 支持 Nacos 服务注册与发现，服务启动时自动注册，停止时自动注销。
 *
 * @author gwyg
 * @since 1.0.0
 */
public class ThriftServerBootstrap implements SmartLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThriftServerBootstrap.class);

    /** Thrift 服务端配置 */
    private ThriftServiceConfig config;

    /** Nacos 配置属性 */
    private NacosConfigProperties nacosConfig;

    /** 服务器运行状态标志 */
    private volatile boolean running = false;

    /** Thrift 服务器实例 */
    private TServer server;
    /**
     * 启动 Thrift 服务器。
     * <p>
     * 如果服务器未运行，则：
     * 1. 创建并配置 Thrift 服务器
     * 2. 启动服务器线程
     * 3. 如果启用了 Nacos，注册服务到注册中心
     *
     * @throws ServiceRegistrationException 如果服务注册失败
     */
    @Override
    public void start() {
        if (!running) {
            int port = config.getPort();
            LOGGER.info("正在启动 Thrift 服务器，端口：{}", port);

            try {
                // 创建传输层
                TNonblockingServerSocket serverTransport = new TNonblockingServerSocket(port);

                // 配置协议工厂
                TProtocolFactory protocolFactory = new TBinaryProtocol.Factory();

                // 配置服务器参数
                TNonblockingServer.Args args = new TNonblockingServer.Args(serverTransport)
                        .transportFactory(new TFramedTransport.Factory())
                        .protocolFactory(protocolFactory)
                        .processor(ThriftServiceProcessor.processor);

                // 创建服务器实例
                server = new TNonblockingServer(args);

                // 启动服务线程
                Thread serverThread = new Thread(() -> {
                    LOGGER.info(ThriftConstants.SERVER_START_SUCCESS_MESSAGE + "，端口：{}", port);
                    server.serve();
                }, ThriftConstants.THRIFT_SERVER_THREAD_NAME);

                serverThread.setDaemon(true);
                serverThread.start();

                running = true;
                LOGGER.info("Thrift 服务器启动线程已启动");

            } catch (Exception e) {
                String errorMessage = ThriftConstants.SERVER_START_FAILURE_MESSAGE + "，端口：" + port;
                LOGGER.error(errorMessage, e);
                throw new ServiceRegistrationException(errorMessage, e);
            }

            // 如果启用了 Nacos，注册服务
            if (nacosConfig != null && nacosConfig.isEnabled()) {
                try {
                    registerToNacos();
                    LOGGER.info("Thrift 服务已注册到 Nacos，服务名：{}", nacosConfig.getServiceName());
                } catch (Exception e) {
                    String errorMessage = ThriftConstants.NACOS_REGISTER_FAILURE_MESSAGE + "，服务名：" + nacosConfig.getServiceName();
                    LOGGER.error(errorMessage, e);
                    throw new ServiceRegistrationException(errorMessage, e);
                }
            }
        } else {
            LOGGER.warn("Thrift 服务器已经在运行中，忽略重复启动请求");
        }
    }
    /**
     * 注册服务到 Nacos 注册中心。
     *
     * @throws Exception 如果注册失败
     */
    private void registerToNacos() throws Exception {
        String serviceName = nacosConfig.getServiceName();
        int port = config.getPort();
        String ip = InetAddress.getLocalHost().getHostAddress();

        LOGGER.debug("正在注册服务到 Nacos，服务名：{}，地址：{}:{}", serviceName, ip, port);

        NamingService namingService = nacosConfig.getNamingService();
        if (namingService == null) {
            throw new IllegalStateException("Nacos 命名服务未初始化");
        }

        Instance instance = new Instance();
        instance.setIp(ip);
        instance.setPort(port);
        instance.setHealthy(true);     // 设置状态为健康
        instance.setEphemeral(true);   // 临时实例，依赖心跳保持

        namingService.registerInstance(serviceName, instance);

        LOGGER.debug("服务注册到 Nacos 成功，服务名：{}，地址：{}:{}", serviceName, ip, port);
    }

    /**
     * 从 Nacos 注册中心注销服务。
     *
     * @throws Exception 如果注销失败
     */
    private void deregisterFromNacos() throws Exception {
        String serviceName = nacosConfig.getServiceName();
        int port = config.getPort();
        String ip = InetAddress.getLocalHost().getHostAddress();
        String group = nacosConfig.getGroup();

        LOGGER.debug("正在从 Nacos 注销服务，服务名：{}，组：{}，地址：{}:{}", serviceName, group, ip, port);

        NamingService namingService = NacosFactory.createNamingService(nacosConfig.getNacosAddr());
        namingService.deregisterInstance(serviceName, group, ip, port);

        LOGGER.debug("服务从 Nacos 注销成功，服务名：{}，地址：{}:{}", serviceName, ip, port);
    }

    /**
     * 停止 Thrift 服务器。
     * <p>
     * 如果服务器正在运行，则：
     * 1. 停止 Thrift 服务器
     * 2. 如果启用了 Nacos，从注册中心注销服务
     *
     * @throws ServiceRegistrationException 如果服务注销失败
     */
    @Override
    public void stop() {
        if (running) {
            int port = config.getPort();
            LOGGER.info("正在停止 Thrift 服务器，端口：{}", port);

            // 停止 Thrift 服务器
            server.stop();
            running = false;
            LOGGER.info(ThriftConstants.SERVER_STOP_MESSAGE + "，端口：{}", port);

            // 如果启用了 Nacos，注销服务
            if (nacosConfig != null && nacosConfig.isEnabled()) {
                try {
                    deregisterFromNacos();
                    LOGGER.info("Thrift 服务已从 Nacos 注销，服务名：{}", nacosConfig.getServiceName());
                } catch (Exception e) {
                    String errorMessage = ThriftConstants.NACOS_DEREGISTER_FAILURE_MESSAGE + "，服务名：" + nacosConfig.getServiceName();
                    LOGGER.error(errorMessage, e);
                    throw new ServiceRegistrationException(errorMessage, e);
                }
            }
        } else {
            LOGGER.warn("Thrift 服务器未在运行中，忽略停止请求");
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
