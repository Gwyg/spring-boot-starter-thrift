package com.huang.thrift.utils;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.huang.thrift.config.NacosConfigProperties;
import com.huang.thrift.config.ThriftConstants;

public class NacosServiceDiscovery implements ServiceDiscovery {

    private static final Logger LOGGER = LoggerFactory.getLogger(NacosServiceDiscovery.class);

    /** Nacos 配置属性 */
    private final NacosConfigProperties nacosConfig;

    // 服务实例缓存：serviceName -> instance list
    private final ConcurrentHashMap<String, List<Instance>> instanceCache = new ConcurrentHashMap<>();


    private final Set<String> subscribedServices = ConcurrentHashMap.newKeySet();

    private final ConcurrentHashMap<String, EventListener> listenerMap = new ConcurrentHashMap<>();


    public NacosServiceDiscovery(NacosConfigProperties nacosConfig) {
        this.nacosConfig = nacosConfig;
        LOGGER.info("NacosServiceDiscovery 初始化完成，采用订阅模式监听服务实例变化");
    }




    @Override
    public List<Instance> getInstances(String serviceName) {
        subscribeIfNecessary(serviceName);

        List<Instance> instances = instanceCache.get(serviceName);
        if (instances != null && !instances.isEmpty()) {
            return instances;
        }

        // 兜底：订阅后缓存仍为空，主动拉一次
        refreshInstances(serviceName);

        List<Instance> refreshed = instanceCache.get(serviceName);
        return refreshed != null ? refreshed : Collections.emptyList();
    }

    @Override
    public boolean hasInstances(String serviceName) {
        List<Instance> instances = getInstances(serviceName);
        return instances != null && !instances.isEmpty();
    }

    @Override
    public void refreshInstances(String serviceName) {
        try {
            List<Instance> newInstances = nacosConfig.getNamingService()
                    .getAllInstances(serviceName, true);

            updateCache(serviceName, newInstances);
        } catch (NacosException e) {
            LOGGER.error("主动刷新服务 '{}' 实例失败", serviceName, e);
            throw new IllegalStateException("Failed to refresh instances for service: " + serviceName, e);
        }

    }

    /**
     * 确保服务已订阅，只订阅一次
     */
    private void subscribeIfNecessary(String serviceName) {
        if (subscribedServices.contains(serviceName)) {
            return;
        }

        synchronized (this) {
            if (subscribedServices.contains(serviceName)) {
                return;
            }

            try {
                EventListener listener = event -> handleNamingEvent(serviceName, event);

                nacosConfig.getNamingService().subscribe(serviceName, listener);
                listenerMap.put(serviceName, listener);
                subscribedServices.add(serviceName);

                LOGGER.info("已订阅 Nacos 服务：{}", serviceName);

                // 订阅后主动初始化一次缓存
                refreshInstances(serviceName);

            } catch (NacosException e) {
                LOGGER.error("订阅服务 '{}' 失败", serviceName, e);
                throw new IllegalStateException("Failed to subscribe service: " + serviceName, e);
            }
        }
    }

    /**
     * 处理 Nacos 推送的服务变更事件
     */
    private void handleNamingEvent(String serviceName, Event event) {
        if (!(event instanceof NamingEvent namingEvent)) {
            LOGGER.debug("忽略非 NamingEvent 事件：{}", event);
            return;
        }

        List<Instance> instances = namingEvent.getInstances();
        updateCache(serviceName, instances);

        LOGGER.info("收到 Nacos 服务变更通知，服务：{}，当前健康实例数：{}",
                serviceName, instances == null ? 0 : instances.size());
    }

    /**
     * 更新本地缓存
     */
    private void updateCache(String serviceName, List<Instance> instances) {
        if (instances == null || instances.isEmpty()) {
            LOGGER.warn("服务 '{}' 当前没有健康实例，清除本地缓存", serviceName);
            instanceCache.remove(serviceName);
        } else {
            instanceCache.put(serviceName, Collections.unmodifiableList(instances));
            LOGGER.debug("服务 '{}' 缓存已更新，实例数：{}", serviceName, instances.size());
        }
    }

    @Override
    public void destroy() {
        LOGGER.info("正在取消 Nacos 服务订阅...");

        listenerMap.forEach((serviceName, listener) -> {
            try {
                nacosConfig.getNamingService().unsubscribe(serviceName, listener);
                LOGGER.info("已取消订阅服务：{}", serviceName);
            } catch (NacosException e) {
                LOGGER.warn("取消订阅服务 '{}' 失败", serviceName, e);
            }
        });

        listenerMap.clear();
        subscribedServices.clear();
        instanceCache.clear();

        LOGGER.info("NacosServiceDiscovery 已关闭");
    }

}
