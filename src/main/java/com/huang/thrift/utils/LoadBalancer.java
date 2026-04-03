package com.huang.thrift.utils;

import com.alibaba.nacos.api.naming.pojo.Instance;
import org.springframework.cloud.client.ServiceInstance;

import java.util.List;

public interface LoadBalancer {
    Instance choose(String serviceName);

}
