package com.huang.thrift.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

//@ConfigurationProperties(prefix = "thrift.client")
public class ThriftClientProperties {
    private String defaultHost = "localhost";
    private int defaultPort = 9000;
    private int defaultTimeout = 3000;
    // Getters & Setters
}
