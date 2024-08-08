package com.samoyer.rpc.proxy;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.samoyer.common.model.po.User;
import com.samoyer.rpc.RpcApplication;
import com.samoyer.rpc.config.RpcConfig;
import com.samoyer.rpc.model.RpcRequest;
import com.samoyer.rpc.model.RpcResponse;
import com.samoyer.rpc.serializer.JdkSerializer;
import com.samoyer.rpc.serializer.Serializer;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * 服务代理（JDK动态代理）
 */
public class ServiceProxy implements InvocationHandler {
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //利用SPI(service provider interface)机制，实现模块化开发和插件化扩展
        //指定序列化器
        Serializer serializer = new JdkSerializer();

        //构建请求
        RpcRequest request = RpcRequest.builder()
                .serviceName(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .parameterTypes(method.getParameterTypes())
                .args(args)
                .build();

        try {
            byte[] bodyBytes = serializer.serialize(request);
            byte[] result;
            //发送请求
            //动态获取配置文件中的主机名和端口号
            RpcConfig uriConfig = RpcApplication.getRpcConfig();
            try (HttpResponse response = HttpRequest.post("http://" +
                                                                    uriConfig.getServerHost() +
                                                                    ":" +
                                                                    uriConfig.getServerPort())
                    .body(bodyBytes)
                    .execute()) {
                //获取响应结果(字节结果)
                result = response.bodyBytes();
            }
            //反序列化响应结果
            RpcResponse rpcResponse = serializer.deserialize(result, RpcResponse.class);

            return rpcResponse.getData();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
