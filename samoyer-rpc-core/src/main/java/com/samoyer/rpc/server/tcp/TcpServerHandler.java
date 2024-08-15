package com.samoyer.rpc.server.tcp;

import com.samoyer.rpc.model.RpcRequest;
import com.samoyer.rpc.model.RpcResponse;
import com.samoyer.rpc.protocol.ProtocolMessage;
import com.samoyer.rpc.protocol.ProtocolMessageDecoder;
import com.samoyer.rpc.protocol.ProtocolMessageEncoder;
import com.samoyer.rpc.protocol.ProtocolMessageTypeEnum;
import com.samoyer.rpc.registry.LocalRegistry;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * TCP请求处理
 * 通过实现Handler<NetSocket>接口来自定义TCP请求处理器
 *
 * @author Samoyer
 * @since 2024-08-14
 */
@Slf4j
public class TcpServerHandler implements Handler<NetSocket> {
    /**
     * 处理TCP请求
     *
     * @param socket
     */
    @Override
    public void handle(NetSocket socket) {
        //处理连接
        TcpBufferHandlerWrapper bufferHandlerWrapper=new TcpBufferHandlerWrapper(buffer -> {
            //接收请求，解码
            ProtocolMessage<RpcRequest> protocolMessage;
            try {
                protocolMessage = (ProtocolMessage<RpcRequest>) ProtocolMessageDecoder.decode(buffer);
            } catch (IOException e) {
                throw new RuntimeException("服务端-协议消息解码错误", e);
            }
            //获取消息体(请求数据)
            RpcRequest rpcRequest = protocolMessage.getBody();
            System.out.println("接收到请求消息: { 服务名称 :" + rpcRequest.getServiceName() +
                    " 方法名称 : " + rpcRequest.getMethodName() + "}");

            //处理请求
            //构造响应结果对象
            RpcResponse rpcResponse = new RpcResponse();
            try {
                //获取要调用的服务实现类，通过反射调用
                Class<?> implClass = LocalRegistry.get(rpcRequest.getServiceName());
                //获取要调用的方法
                Method method = implClass.getMethod(rpcRequest.getMethodName(), rpcRequest.getParameterTypes());
                //通过反射调用
                Object result = method.invoke(implClass.getDeclaredConstructor().newInstance(), rpcRequest.getArgs());

                //封装返回结果
                rpcResponse.setData(result);
                rpcResponse.setDataType(method.getReturnType());
                rpcResponse.setMessage("OK");
            } catch (Exception e) {
                log.error("调用服务实现类失败",e);
                rpcResponse.setMessage(e.getMessage());
                rpcResponse.setException(e);
            }

            //准备编码响应结果
            ProtocolMessage.Header header = protocolMessage.getHeader();
            //把request的请求头直接拿过来用，改一下type为响应就行
            header.setType((byte) ProtocolMessageTypeEnum.RESPONSE.getKey());
            //构建消息
            ProtocolMessage<RpcResponse> rpcResponseProtocolMessage = new ProtocolMessage<>(header, rpcResponse);
            try {
                //编码
                Buffer encodeBuffer = ProtocolMessageEncoder.encode(rpcResponseProtocolMessage);
                //发送响应
                socket.write(encodeBuffer);
            } catch (IOException e) {
                throw new RuntimeException("服务端-协议消息编码错误",e);
            }
        });

        socket.handler(bufferHandlerWrapper);
    }
}
