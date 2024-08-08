package com.samoyer.rpc.server;

import io.vertx.core.Vertx;

/**
 * 基于Vert.x实现的web服务器VertxHttpServer
 * 监听指定端口并处理请求
 */
public class VertxHttpServer implements HttpServer{
    @Override
    public void doStart(int port) {
        //创建Vert.x实例
        Vertx vertx=Vertx.vertx();
        //创建HTTP服务器
        io.vertx.core.http.HttpServer server=vertx.createHttpServer();

        //监听端口并处理请求
        server.requestHandler(
            /*
            request -> {
            //处理HTTP请求
            System.out.println("接收到request method："+request.method()
                    +" uri: "+request.uri());

            //发送HTTP响应
            request.response()
                    .putHeader("content-type","text/plain")
                    .end("Response from Vert.x HTTP Server!");
            }
            */

            //自定义请求处理器
            new HttpServerHandler()
        );

        //启动HTTP服务器并监听端口
        server.listen(port,result -> {
            if (result.succeeded()){
                System.out.println("Server is now listening on port "+port);
            }else {
                System.out.println("Failed to start server: "+result.cause());
            }
        });
    }
}
