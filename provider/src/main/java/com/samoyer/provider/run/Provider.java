package com.samoyer.provider.run;

import com.samoyer.common.service.UserService;
import com.samoyer.provider.service.UserServiceImpl;
import com.samoyer.rpc.RpcApplication;
import com.samoyer.rpc.config.RegistryConfig;
import com.samoyer.rpc.config.RpcConfig;
import com.samoyer.rpc.model.ServiceMetaInfo;
import com.samoyer.rpc.registry.LocalRegistry;
import com.samoyer.rpc.registry.Registry;
import com.samoyer.rpc.registry.RegistryFactory;
import com.samoyer.rpc.server.HttpServer;
import com.samoyer.rpc.server.VertxHttpServer;
import com.samoyer.rpc.server.tcp.VertxTcpServer;

/**
 * 简单的服务提供者
 *
 * @author Samoyer
 */
public class Provider {
    public static void main(String[] args) {
        //RPC框架初始化，加载相关配置
        RpcApplication.init();

        //注册服务
        String serviceName = UserService.class.getName();
        LocalRegistry.register(serviceName, UserServiceImpl.class);

        //注册服务到注册中心
        //获取application.properties中的相关配置
        RpcConfig rpcConfig = RpcApplication.getRpcConfig();
        //获取注册中心的配置
        RegistryConfig registryConfig = rpcConfig.getRegistryConfig();
        //根据key=etcd获取其实例（EtcdRegistry）
        Registry registry = RegistryFactory.getInstance(registryConfig.getRegistry());
        //设置此服务要注册的元信息
        ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
        serviceMetaInfo.setServiceName(serviceName);
        serviceMetaInfo.setServiceVersion(rpcConfig.getVersion());
        serviceMetaInfo.setServiceHost(rpcConfig.getServerHost());
        serviceMetaInfo.setServicePort(rpcConfig.getServerPort());
        try {
            registry.register(serviceMetaInfo);
        } catch (Exception e) {
            throw new RuntimeException(serviceName+"注册服务失败",e);
        }

        //启动HTTP服务
//        HttpServer httpServer=new VertxHttpServer();
//        httpServer.doStart(RpcApplication.getRpcConfig().getServerPort());

        //启动TCP服务
        VertxTcpServer vertxTcpServer = new VertxTcpServer();
        vertxTcpServer.doStart(RpcApplication.getRpcConfig().getServerPort());
    }
}
