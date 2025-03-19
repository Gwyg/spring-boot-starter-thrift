package com.huang.thrift.utils;


import com.alibaba.nacos.api.naming.pojo.Instance;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;


public class RandomLoadBalancer implements LoadBalancer{
    // nacos 服务实例缓存
    private ConcurrentHashMap<String, List<Instance>> instances = new ConcurrentHashMap<>();
    private final Random random = new Random();

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
