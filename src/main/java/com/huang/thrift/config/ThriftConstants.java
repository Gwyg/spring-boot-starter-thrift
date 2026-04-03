package com.huang.thrift.config;

/**
 * Thrift Starter 常量定义类
 * <p>
 * 包含项目中使用的所有硬编码常量值，便于统一管理和配置。
 *
 * @author gwyg
 * @since 1.0.0
 */
public final class ThriftConstants {

    /** 私有构造方法，防止实例化 */
    private ThriftConstants() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }

    // ==================== 网络相关常量 ====================

    /** 默认服务端主机地址 */
    public static final String DEFAULT_SERVER_HOST = "localhost";

    /** 默认服务端端口号 */
    public static final int DEFAULT_SERVER_PORT = 9000;

    /** 默认连接超时时间（毫秒） */
    public static final int DEFAULT_CONNECTION_TIMEOUT = 3000;

    // ==================== Nacos 相关常量 ====================

    /** 默认 Nacos 服务器地址 */
    public static final String DEFAULT_NACOS_ADDR = "127.0.0.1:8848";

    /** 默认 Nacos 服务名称 */
    public static final String DEFAULT_NACOS_SERVICE_NAME = "thrift-server";

    /** 默认 Nacos 客户端服务名称 */
    public static final String DEFAULT_NACOS_CLIENT_SERVICE_NAME = "thrift-service";

    /** Nacos 实例健康检查间隔（秒） */
    public static final int NACOS_HEALTH_CHECK_INTERVAL = 10;

    // ==================== 连接池相关常量 ====================

    /** 连接池最大连接数 */
    public static final int CONNECTION_POOL_MAX_TOTAL = 5;

    /** 连接池最大空闲连接数 */
    public static final int CONNECTION_POOL_MAX_IDLE = 3;

    /** 连接池最小空闲连接数 */
    public static final int CONNECTION_POOL_MIN_IDLE = 0;

    /** 连接池空闲对象检测间隔（秒） */
    public static final long CONNECTION_POOL_EVICTION_RUN_INTERVAL = 30L;

    /** 连接池最小空闲时间（秒） */
    public static final long CONNECTION_POOL_MIN_EVICTABLE_IDLE_TIME = 60L;

    // ==================== 注解默认值 ====================

    /** ThriftClient 注解默认端口值（表示使用默认端口） */
    public static final int ANNOTATION_DEFAULT_PORT = -1;

    /** 默认服务名称占位符 */
    public static final String DEFAULT_SERVICE_NAME_PLACEHOLDER = "";

    // ==================== 服务器相关常量 ====================

    /** Thrift 服务端线程名称 */
    public static final String THRIFT_SERVER_THREAD_NAME = "thrift-server";

    /** 服务启动成功消息 */
    public static final String SERVER_START_SUCCESS_MESSAGE = "Thrift 服务器启动成功";

    /** 服务关闭消息 */
    public static final String SERVER_STOP_MESSAGE = "Thrift 服务器已关闭";

    // ==================== 错误消息常量 ====================

    /** Nacos 初始化失败消息 */
    public static final String NACOS_INIT_FAILURE_MESSAGE = "Nacos 初始化失败！";

    /** Nacos 注册失败消息 */
    public static final String NACOS_REGISTER_FAILURE_MESSAGE = "Nacos 注册失败！";

    /** Nacos 注销失败消息 */
    public static final String NACOS_DEREGISTER_FAILURE_MESSAGE = "Nacos 注销失败！";

    /** 服务名称重复错误消息模板 */
    public static final String DUPLICATE_SERVICE_NAME_MESSAGE = "服务名称重复: %s";

    /** 服务接口缺失错误消息模板 */
    public static final String MISSING_IFACE_INTERFACE_MESSAGE = "服务必须实现 $Iface 接口: %s";

    /** Thrift 服务器启动失败消息 */
    public static final String SERVER_START_FAILURE_MESSAGE = "Thrift 服务器启动失败";

    /** Thrift 客户端创建失败消息模板 */
    public static final String CLIENT_CREATE_FAILURE_MESSAGE = "Failed to create Thrift client";

    /** 无法获取 Nacos 实例错误消息模板 */
    public static final String NO_NACOS_INSTANCES_MESSAGE = "不能从 Nacos 获取服务实例: %s";

    /** 未定义服务降级错误消息 */
    public static final String NO_FALLBACK_DEFINED_MESSAGE = "没有定义服务降级";

    /** 无法创建 Fallback 对象错误消息 */
    public static final String FALLBACK_CREATION_FAILURE_MESSAGE = "不能创建 Fallback 对象";

    /** Nacos 实例刷新失败消息模板 */
    public static final String NACOS_INSTANCE_REFRESH_FAILURE_MESSAGE = "刷新实例列表失败: %s";

    // ==================== 协议相关常量 ====================

    /** Thrift 多路复用协议分隔符 */
    public static final String MULTIPLEXED_PROTOCOL_SEPARATOR = ":";

    /** 连接池键值分隔符 */
    public static final String CONNECTION_POOL_KEY_SEPARATOR = ":";

    // ==================== 熔断相关常量 ====================

    /** Sentinel 资源名称分隔符 */
    public static final String SENTINEL_RESOURCE_SEPARATOR = "#";
}