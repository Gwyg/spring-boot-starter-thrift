package com.huang.thrift.utils;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.huang.thrift.config.NacosConfigProperties;
import com.huang.thrift.config.ThriftConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;


/**
 * 随机负载均衡器实现类。
 * <p>
 * 从 Nacos 服务注册中心获取服务实例，并采用随机算法选择实例。
 * 定期（默认10秒）刷新服务实例缓存，确保只使用健康实例。
 * 实现了 {@link DisposableBean} 接口，确保 Spring 容器关闭时正确释放资源。
 *
 * @author gwyg
 * @since 1.0.0
 */
public class RandomLoadBalancer implements LoadBalancer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RandomLoadBalancer.class);

    private final ServiceDiscovery serviceDiscovery;

    public RandomLoadBalancer(ServiceDiscovery serviceDiscovery) {
        this.serviceDiscovery = serviceDiscovery;
    }


    @Override
    public Instance choose(String serviceName) {
        List<Instance> instanceList = serviceDiscovery.getInstances(serviceName);

        if (instanceList == null || instanceList.isEmpty()) {
            String errorMessage = String.format(ThriftConstants.NO_NACOS_INSTANCES_MESSAGE, serviceName);
            LOGGER.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        Instance selectedInstance = instanceList.get(
                ThreadLocalRandom.current().nextInt(instanceList.size())
        );

        LOGGER.debug("为服务 '{}' 随机选择实例：{}:{}",
                serviceName, selectedInstance.getIp(), selectedInstance.getPort());

        return selectedInstance;
    }
}
