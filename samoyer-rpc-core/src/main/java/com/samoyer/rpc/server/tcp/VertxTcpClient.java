package com.samoyer.rpc.server.tcp;

import cn.hutool.core.util.IdUtil;
import com.samoyer.rpc.RpcApplication;
import com.samoyer.rpc.model.RpcRequest;
import com.samoyer.rpc.model.RpcResponse;
import com.samoyer.rpc.model.ServiceMetaInfo;
import com.samoyer.rpc.protocol.*;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * TCP客户端
 *
 * @author Samoyer
 * @since 2024-08-14
 */
@Slf4j
public class VertxTcpClient {
    /**
     * 发送请求
     * （封装）
     *
     * @param rpcRequest
     * @param serviceMetaInfo
     * @return
     */
    public static RpcResponse doRequest(RpcRequest rpcRequest, ServiceMetaInfo serviceMetaInfo) throws ExecutionException, InterruptedException {
        //基于TCP发送请求
        Vertx vertx = Vertx.vertx();
        NetClient netClient = vertx.createNetClient();
        //Vert.x提供的请求处理器是异步的，为了方便获取结果，使用CompletableFuture转为同步
        CompletableFuture<RpcResponse> responseFuture = new CompletableFuture<>();
        netClient.connect(serviceMetaInfo.getServicePort(),
                serviceMetaInfo.getServiceHost(),
                result -> {
                    if (result.succeeded()) {
                        log.info("连接到TCP服务器");
                        NetSocket socket = result.result();
                        //发送数据
                        //构造消息
                        ProtocolMessage<RpcRequest> protocolMessage = new ProtocolMessage<>();
                        ProtocolMessage.Header header = new ProtocolMessage.Header();
                        header.setMagic(ProtocolConstant.PROTOCOL_MAGIC);
                        header.setVersion(ProtocolConstant.PROTOCOL_VERSION);
                        header.setSerializer((byte) Objects.requireNonNull(
                                        ProtocolMessageSerializerEnum.getEnumByValue(
                                                RpcApplication.getRpcConfig().getSerializer()
                                        )
                                ).getKey()
                        );
                        header.setType((byte) ProtocolMessageTypeEnum.REQUEST.getKey());
                        //生成全局请求ID
                        header.setRequestId(IdUtil.getSnowflakeNextId());
                        protocolMessage.setHeader(header);
                        protocolMessage.setBody(rpcRequest);

                        //编码
                        try {
                            log.info("编码请求消息中...");
                            Buffer encodeBuffer = ProtocolMessageEncoder.encode(protocolMessage);
                            socket.write(encodeBuffer);
                            log.info("***请求消息发送成功***");
                        } catch (IOException e) {
                            throw new RuntimeException("消费端-协议消息编码错误");
                        }

                        //接收响应;使用TcpBufferHandlerWrapper先对buffer进行处理（防粘包半包）
                        TcpBufferHandlerWrapper bufferHandlerWrapper = new TcpBufferHandlerWrapper(buffer -> {
                            try {
                                ProtocolMessage<RpcResponse> rpcResponseProtocolMessage =
                                        (ProtocolMessage<RpcResponse>)
                                                ProtocolMessageDecoder.decode(buffer);
                                //完成响应
                                responseFuture.complete(rpcResponseProtocolMessage.getBody());
                            } catch (IOException e) {
                                throw new RuntimeException("消费端-协议消息解码错误");
                            }
                        });

                        socket.handler(bufferHandlerWrapper);
                    } else {
                        log.error("连接到TCP服务器失败");
                    }
                }
        );

        //阻塞，等到响应完成，才继续执行
        RpcResponse rpcResponse = responseFuture.get();
        //关闭连接
        netClient.close();

//        基于HTTP发送请求
//            byte[] result;
//            //发送请求
//            try (HttpResponse response = HttpRequest.post(serviceMetaInfo.getServiceAddress())
//                    .body(bodyBytes)
//                    .execute()) {
//                //获取响应结果(字节结果)
//                result = response.bodyBytes();
//            }
//            //反序列化响应结果
//            RpcResponse rpcResponse = serializer.deserialize(result, RpcResponse.class);

        return rpcResponse;
    }

    /**
     * 测试用
     */
    private void start() {
        //创建vert.x实例
        Vertx vertx = Vertx.vertx();

        vertx.createNetClient().connect(8888, "localhost", result -> {
            //连接成功
            if (result.succeeded()) {
                System.out.println("成功连接到TCP服务器");
                NetSocket socket = result.result();
                //模拟发送1000次消息
                for (int i = 0; i < 1000; i++) {
                    //发送数据
                    Buffer buffer = Buffer.buffer();
                    String str = "Hello,server!Hello,server!Hello,server!Hello,server!";
                    //构造8个字节的请求头
                    buffer.appendInt(0);
                    buffer.appendInt(str.getBytes().length);
                    //构造请求体
                    buffer.appendBytes(str.getBytes());
                    //向服务端发送数据
                    socket.write(buffer);
                }
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
