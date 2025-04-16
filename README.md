# springboot 集成 Thrift 的 starter
基于Spring Boot的Thrift集成框架，提供注解驱动的Thrift服务端/客户端自动配置，支持Nacos服务发现、连接池及熔断功能。

## 功能特性

- **注解驱动**：通过`@ThriftService`快速注册服务，`@ThriftClient`自动注入客户端
- **服务发现**：集成 Nacos 实现服务注册与发现，支持动态实例更新
- **负载均衡**：内置随机负载均衡策略，可扩展其他策略
- **连接池**：使用Apache Commons Pool 2管理Thrift连接
- **熔断降级**：集成Sentinel实现熔断机制，支持Fallback类

## 快速开始

导入依赖
```xml
<dependency>
     <groupId>io.github.gwyg</groupId>
     <artifactId>thrift-spring-boot-starter</artifactId>
     <version>1.0.5</version>
</dependency>
```

### 服务端配置

1. 定义Thrift接口（示例`HelloService.thrift`）：
```thrift
service HelloService {
    string sayHello(1:string name)
}
```
2. 实现服务并添加注解：  

可以通过注解配置服务名字,需要与客户端注解相对应,不写默认为helloService,Iface接口的外部类名字首字母小写
```java
@ThriftService
public class HelloServiceImpl implements HelloService.Iface {
    @Override
    public String sayHello(String name) {
        return "Hello " + name;
    }
}
```
3. 配置application.yml：

```yaml
spring:
  thrift:
    service:
      enabled: true # 打开服务端功能
      port: 9000 # 监听端口
    nacos:
      enabled: true # 打开Nacos服务发现功能(可选)
      nacos-addr: 127.0.0.1:8848
      service-name: hello-service #注册到Nacos的服务名
```
## 客户端配置
1. 注入客户端：
```java
@RestController
public class DemoController {
    // 注解内的serviceName和nacosName需要与服务端配置相对应,不写有默认值
    // serviceName 默认为属性名 nacosName 默认为 thrift-server
    @ThriftClient
    private HelloService.Iface helloService;
    
    @GetMapping("/hello")
    public String hello2() throws TException {
        return helloService.sayHello("我是HelloService.Iface");
    }
}
```
2. 客户端配置（可选）：
```yaml
spring:
  thrift:
    client:
      server-host: localhost # 服务端地址
      server-port: 9000 # 服务端端口
      timeout: 3000 # 请求超时时间
      circuit-breaker-enabled: true # 熔断开关,需要一些额外的配置
```
启动服务测试即可


