package com.samoyer.rpc.server.tcp;

import com.samoyer.rpc.server.HttpServer;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetServer;

/**
 * TCP服务端
 * 本RPC框架使用的Vert.x作为网络传输服务器，之前用的是HttpServer
 * 现在改用TCP
 *
 * @author Samoyer
 * @since 2024-08-14
 */
public class VertxTcpServer implements HttpServer {

    private byte[] handleRequest(byte[] requestData) {
        //在这里编写处理请求的逻辑，根据requestData构造响应数据并返回
        return "Hello,client!".getBytes();
    }

    @Override
    public void doStart(int port) {
        //创建Vert.x实例
        Vertx vertx = Vertx.vertx();

        //创建TCP服务器
        NetServer server = vertx.createNetServer();

        //监听端口并处理请求
        server.connectHandler(new TcpServerHandler());

        //启动HTTP服务器并监听端口
        server.listen(port, result -> {
            if (result.succeeded()) {
                System.out.println("TCP server is now listening on port " + port);
            } else {
                System.out.println("Failed to start TCP server: " + result.cause());
            }
        });
    }
}
