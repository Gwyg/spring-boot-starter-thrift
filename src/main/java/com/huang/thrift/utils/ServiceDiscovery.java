package com.huang.thrift.utils;

import java.util.List;

import com.alibaba.nacos.api.naming.pojo.Instance;

public interface ServiceDiscovery {
    List<Instance> getInstances(String serviceName);
    boolean hasInstances(String serviceName);

    void refreshInstances(String serviceName);

    void destroy();
}
