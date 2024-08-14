package com.samoyer.rpc.server.tcp;

import io.vertx.core.Vertx;
import io.vertx.core.net.NetSocket;

/**
 * TCP客户端
 *
 * @author Samoyer
 * @since 2024-08-14
 */
public class VertxTcpClient {
    public void start() {
        //创建vert.x实例
        Vertx vertx = Vertx.vertx();

        vertx.createNetClient().connect(8888, "localhost", result -> {
            //连接成功
            if (result.succeeded()) {
                System.out.println("成功连接到TCP服务器");
                NetSocket socket = result.result();
                //向服务端发送数据
                socket.write("Hello,server!");
                //接收响应
                socket.handler(buffer -> {
                    System.out.println("接收到来自服务端的响应：" + buffer.toString());
                });
            } else {
                System.out.println("连接到TCP服务器失败");
            }
        });
    }
}
