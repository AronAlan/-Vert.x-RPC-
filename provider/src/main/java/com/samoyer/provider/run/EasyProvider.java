package com.samoyer.provider.run;

import com.samoyer.common.service.UserService;
import com.samoyer.provider.service.UserServiceImpl;
import com.samoyer.rpc.registry.LocalRegistry;
import com.samoyer.rpc.server.HttpServer;
import com.samoyer.rpc.server.VertxHttpServer;

/**
 * 简单的服务提供者
 */
public class EasyProvider {
    public static void main(String[] args) {
        //注册服务
        LocalRegistry.register(UserService.class.getName(), UserServiceImpl.class);

        //启动web服务
        HttpServer httpServer=new VertxHttpServer();
        httpServer.doStart(8080);
    }
}
