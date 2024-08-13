package com.samoyer.rpc.config;

import com.samoyer.rpc.serializer.SerializerKeys;
import lombok.Data;

/**
 * RPC框架配置，用于保存配置信息
 * @author Samoyer
 * @since 2024-08-09
 */
@Data
public class RpcConfig {
    /**
     * 名称
     */
    private String name = "samoyer-rpc";

    /**
     * 版本号
     */
    private String version = "1.0";

    /**
     * 服务器主机名
     */
    private String serverHost = "localhost";

    /**
     * 服务器端口号
     */
    private Integer serverPort = 8085;

    /**
     * mock的开启，模拟调用
     */
    private boolean mock=false;

    /**
     * 序列化器
     * 默认使用JDK
     */
    private String serializer= SerializerKeys.JDK;

    /**
     * 注册中心配置
     */
    private RegistryConfig registryConfig=new RegistryConfig();
}
