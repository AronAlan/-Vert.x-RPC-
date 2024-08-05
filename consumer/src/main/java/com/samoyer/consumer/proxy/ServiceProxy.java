package com.samoyer.consumer.proxy;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.samoyer.common.domain.po.User;
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
        //指定序列化器
        Serializer serializer = new JdkSerializer();

        //构建请求
        RpcRequest request=RpcRequest.builder()
                .serviceName(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .parameterTypes(method.getParameterTypes())
                .args(args)
                .build();

        try {
            byte[] bodyBytes = serializer.serialize(request);
            byte[] result;
            //发送请求
            //TODO 地址硬编码改为使用注册中心
            try(HttpResponse response = HttpRequest.post("http://localhost:8080")
                    .body(bodyBytes)
                    .execute()){
                //获取响应结果(字节结果)
                result=response.bodyBytes();
            }
            //反序列化响应结果
            RpcResponse rpcResponse = serializer.deserialize(result, RpcResponse.class);

            return (User) rpcResponse.getData();
        }catch (IOException e){
            e.printStackTrace();
        }

        return null;    }
}
