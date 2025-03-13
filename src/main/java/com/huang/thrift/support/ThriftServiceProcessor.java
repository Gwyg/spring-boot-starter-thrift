package com.huang.thrift.support;

import com.huang.thrift.annotation.ThriftService;
import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.TProcessor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class ThriftServiceProcessor implements BeanPostProcessor {

    private ApplicationContext applicationContext;
    private final Map<String, Class<?>> serviceInterfaceMap = new ConcurrentHashMap<>();
    public static TMultiplexedProcessor processor = new TMultiplexedProcessor();



    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        ThriftService annotation = AnnotationUtils.findAnnotation(bean.getClass(), ThriftService.class);
        // 可以优化，防止注解乱加
        if(annotation != null){
            // 获取服务接口类型
            Class<?> ifaceClass = Arrays.stream(bean.getClass().getInterfaces())
                    .filter(iface -> iface.getName().endsWith("$Iface"))
                    .findFirst()
                    .orElseThrow(() -> new BeanInitializationException("服务必须实现 $Iface 接口: " + beanName));
            // 获取对应的 Processor 类（约定：接口所在包下有对应的 Processor）
            String processorClassName = ifaceClass.getName().replace("$Iface", "$Processor");
            try {
                Class<?> processorClass = Class.forName(processorClassName);
                TProcessor processor = (TProcessor) processorClass.getConstructor(ifaceClass).newInstance(bean);
                // 确定服务名称（优先注解值，其次接口简名）
                String serviceName = annotation.name();
                if(StringUtils.isEmpty(serviceName)){
                    serviceName = ifaceClass.getSimpleName().replace("$Iface", "");
                }
                // 防止重复注册
                if(serviceInterfaceMap.containsKey(serviceName)){
                    throw new BeanInitializationException("服务名称重复: " + serviceName);
                }
                serviceInterfaceMap.put(serviceName, ifaceClass);
                // 注册多路服用器
                this.processor.registerProcessor(serviceName, processor);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return bean;
    }

}


