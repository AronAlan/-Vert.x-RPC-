package com.samoyer.consumer.proxy;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.samoyer.common.model.po.User;
import com.samoyer.common.service.UserService;
import com.samoyer.rpc.model.RpcRequest;
import com.samoyer.rpc.model.RpcResponse;
import com.samoyer.rpc.serializer.JdkSerializer;
import com.samoyer.rpc.serializer.Serializer;

import java.io.IOException;

/**
 * 服务代理（静态代理）
 */
public class UserServiceProxy implements UserService {
    @Override
    public User getUserInfo(User user) {
        //指定序列化器
        Serializer serializer = new JdkSerializer();

        //构建请求
        RpcRequest request=RpcRequest.builder()
                .serviceName(UserService.class.getName())
                .methodName("getUserInfo")
                .parameterTypes(new Class[]{User.class})
                .args(new Object[]{user})
                .build();

        try {
            byte[] bodyBytes = serializer.serialize(request);
            byte[] result;
            //发送请求
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

        return null;
    }
}
