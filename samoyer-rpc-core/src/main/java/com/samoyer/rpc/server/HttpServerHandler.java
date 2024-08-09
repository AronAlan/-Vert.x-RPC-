package com.samoyer.rpc.server;

import com.samoyer.rpc.RpcApplication;
import com.samoyer.rpc.model.RpcRequest;
import com.samoyer.rpc.model.RpcResponse;
import com.samoyer.rpc.registry.LocalRegistry;
import com.samoyer.rpc.serializer.JdkSerializer;
import com.samoyer.rpc.serializer.Serializer;
import com.samoyer.rpc.serializer.SerializerFactory;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * HTTP请求处理
 * Vert.x通过实现Handler<HttpServerRequest>接口来自定义请求处理器
 * @author Samoyer
 */
public class HttpServerHandler implements Handler<HttpServerRequest> {
    /**
     * 处理HTTP请求
     *
     * @param request
     */
    @Override
    public void handle(HttpServerRequest request) {
        //指定序列化器
        /*final Serializer serializer = new JdkSerializer();*/
        //动态获取序列化器：读取配置，使用工厂获取序列化器的实例
        final Serializer serializer = SerializerFactory.getInstance(RpcApplication.getRpcConfig().getSerializer());
        System.out.println("序列化器:"+serializer);

        System.out.println("接收到request method:" + request.method() +
                " uri: " + request.uri());

        //异步处理HTTP请求(Vert.x通过request.bodyHandler异步处理请求)
        request.bodyHandler(body -> {
            //获取请求体中的字节数据
            byte[] bytes = body.getBytes();
            RpcRequest rpcRequest = null;
            //反序列化请求为对象
            try {
                rpcRequest = serializer.deserialize(bytes, RpcRequest.class);
            } catch (Exception e) {
                e.printStackTrace();
            }

            //构造响应结果对象
            RpcResponse rpcResponse = new RpcResponse();
            //如果请求为null，直接返回
            if (rpcRequest == null) {
                rpcResponse.setMessage("rpcRequest is null");
                doResponse(request, rpcResponse, serializer);
                return;
            }

            try {
                //获取要调用的服务实现类，通过反射调用
                Class<?> implClass = LocalRegistry.get(rpcRequest.getServiceName());
                //获取要调用的方法
                Method method = implClass.getMethod(rpcRequest.getMethodName(),rpcRequest.getParameterTypes());
                //通过反射调用
                Object result = method.invoke(implClass.getDeclaredConstructor().newInstance(), rpcRequest.getArgs());

                //封装返回结果
                rpcResponse.setData(result);
                rpcResponse.setDataType(method.getReturnType());
                rpcResponse.setMessage("OK");
            }catch (Exception e){
                e.printStackTrace();
                rpcResponse.setMessage(e.getMessage());
                rpcResponse.setException(e);
            }

            //发送响应
            doResponse(request,rpcResponse,serializer);
        });

    }

    /**
     * 响应
     *
     * @param request
     * @param rpcResponse
     * @param serializer
     */
    private void doResponse(HttpServerRequest request, RpcResponse rpcResponse, Serializer serializer) {
        HttpServerResponse httpServerResponse = request.response()
                .putHeader("content-type", "application/json");

        try {
            //序列化
            byte[] serializedResponse = serializer.serialize(rpcResponse);
            httpServerResponse.end(Buffer.buffer(serializedResponse));
        } catch (IOException e) {
            e.printStackTrace();
            httpServerResponse.end(Buffer.buffer());
        }
    }

}
