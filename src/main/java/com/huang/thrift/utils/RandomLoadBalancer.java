package com.huang.thrift.utils;


import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.huang.thrift.config.NacosConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class RandomLoadBalancer implements LoadBalancer {
    private final NacosConfigProperties nacosConfig;
    // nacos 服务实例缓存
    private ConcurrentHashMap<String, List<Instance>> instances = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private final ScheduledExecutorService refreshScheduler = Executors.newSingleThreadScheduledExecutor();

    public RandomLoadBalancer(NacosConfigProperties NacosConfigProperties) {
        this.nacosConfig = NacosConfigProperties;
        refreshScheduler.scheduleAtFixedRate(this::refreshHealthyInstances, 0, 10, TimeUnit.SECONDS);
    }

    // 刷新健康实例
    private void refreshHealthyInstances() {
        // 遍历所有服务名称，获取健康实例
        instances.keySet().forEach(serviceName -> {
            try {
                List<Instance> newInstances = nacosConfig.getNamingService().getAllInstances(
                        serviceName,
                        true // 仅返回健康实例
                );
                updateInstances(serviceName, newInstances);
            } catch (NacosException e) {
                System.err.println("刷新实例列表失败: " + e.getMessage());
            }
        });
    }


    @Override
    public Instance choose(String serviceName) {
        List<Instance> instances = this.instances.get(serviceName);
        if (instances == null || instances.isEmpty()) {
            throw new IllegalStateException("不能从 Nacos 获取服务实例: " + serviceName);
        }
        return instances.get(random.nextInt(instances.size()));
    }

    @Override
    public void updateInstances(String serviceName, List<Instance> instances) {
        this.instances.put(serviceName, instances);
    }

    @Override
    public boolean hasInstances(String serviceName) {
        return instances.containsKey(serviceName);
    }

}
