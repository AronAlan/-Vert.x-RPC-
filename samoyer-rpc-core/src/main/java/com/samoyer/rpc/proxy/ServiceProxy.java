package com.samoyer.rpc.proxy;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import com.samoyer.rpc.RpcApplication;
import com.samoyer.rpc.config.RpcConfig;
import com.samoyer.rpc.constant.RpcConstant;
import com.samoyer.rpc.model.RpcRequest;
import com.samoyer.rpc.model.RpcResponse;
import com.samoyer.rpc.model.ServiceMetaInfo;
import com.samoyer.rpc.protocol.*;
import com.samoyer.rpc.registry.Registry;
import com.samoyer.rpc.registry.RegistryFactory;
import com.samoyer.rpc.serializer.Serializer;
import com.samoyer.rpc.serializer.SerializerFactory;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * 服务代理（JDK动态代理）
 *
 * @author Samoyer
 */
@Slf4j
public class ServiceProxy implements InvocationHandler {
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        log.info("Service执行invoke方法:{}", method.getName());
        //指定序列化器
        /*Serializer serializer = new JdkSerializer();*/
        //利用SPI(service provider interface)机制，实现模块化开发和插件化扩展
        //动态获取序列化器：读取配置，使用工厂获取序列化器的实例
        final Serializer serializer = SerializerFactory.getInstance(RpcApplication.getRpcConfig().getSerializer());
        log.info("加载到序列化器:{}", serializer);

        //构建请求
        String serviceName = method.getDeclaringClass().getName();
        RpcRequest rpcRequest = RpcRequest.builder()
                .serviceName(serviceName)
                .methodName(method.getName())
                .parameterTypes(method.getParameterTypes())
                .args(args)
                .build();

        try {
            //先对请求数据序列化
            byte[] bodyBytes = serializer.serialize(rpcRequest);

            // 从注册中心获取服务提供者的地址（如果已存在，则从缓存中直接读取）
            // 获取配置信息
            RpcConfig rpcConfig = RpcApplication.getRpcConfig();
            // 根据配置信息初始化注册中心
            Registry registry = RegistryFactory.getInstance(rpcConfig.getRegistryConfig().getRegistry());
            // 初始化服务元信息
            ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
            // 设置服务名称
            serviceMetaInfo.setServiceName(serviceName);
            // 设置服务版本为默认版本
            serviceMetaInfo.setServiceVersion(RpcConstant.DEFAULT_SERVICE_VERSION);
            // 服务发现，获取此服务的所有节点（先拉缓存，缓存没有再去服务中心）
            List<ServiceMetaInfo> serviceMetaInfoList = registry.serviceDiscovery(serviceMetaInfo.getServiceKey());
            //在log.info中打印serviceMetaInfoList的元素
            serviceMetaInfoList.forEach(smi ->
                    log.info("发现服务地址:{}", smi.getServiceAddress())
            );
            // 如果服务节点列表为空，抛出异常
            if (CollUtil.isEmpty(serviceMetaInfoList)) {
                throw new RuntimeException("暂无服务地址");
            }
            // 选择第一个服务节点
            ServiceMetaInfo selectedServiceMetaInfo = serviceMetaInfoList.get(0);

            //基于TCP发送请求
            Vertx vertx = Vertx.vertx();
            NetClient netClient = vertx.createNetClient();
            //Vert.x提供的请求处理器是异步的，为了方便获取结果，使用CompletableFuture转为同步
            CompletableFuture<RpcResponse> responseFuture = new CompletableFuture<>();
            netClient.connect(selectedServiceMetaInfo.getServicePort(),
                    selectedServiceMetaInfo.getServiceHost(),
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

                            //接收响应
                            socket.handler(buffer -> {
                                try {
                                    ProtocolMessage<RpcResponse> rpcResponseProtocolMessage = (ProtocolMessage<RpcResponse>) ProtocolMessageDecoder.decode(buffer);
                                    //完成响应
                                    responseFuture.complete(rpcResponseProtocolMessage.getBody());
                                } catch (IOException e) {
                                    throw new RuntimeException("消费端-协议消息解码错误");
                                }
                            });
                        }else {
                            log.error("连接到TCP服务器失败");
                        }
                    }
            );

            //阻塞，等到响应完成，才继续执行
            RpcResponse rpcResponse = responseFuture.get();
            //关闭连接
            netClient.close();

            /* 基于HTTP发送请求
                byte[] result;
                //发送请求
                try (HttpResponse response = HttpRequest.post(selectedServiceMetaInfo.getServiceAddress())
                        .body(bodyBytes)
                        .execute()) {
                    //获取响应结果(字节结果)
                    result = response.bodyBytes();
                }
                //反序列化响应结果
                RpcResponse rpcResponse = serializer.deserialize(result, RpcResponse.class);
             */


            return rpcResponse.getData();
        } catch (IOException e) {
            log.error("消费端向服务端发送消息失败");
        }

        return null;
    }
}
