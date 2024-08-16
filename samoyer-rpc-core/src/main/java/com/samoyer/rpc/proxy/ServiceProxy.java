package com.samoyer.rpc.proxy;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import com.samoyer.rpc.RpcApplication;
import com.samoyer.rpc.config.RpcConfig;
import com.samoyer.rpc.constant.RpcConstant;
import com.samoyer.rpc.loadbalancer.RandomLoadBalancer;
import com.samoyer.rpc.model.RpcRequest;
import com.samoyer.rpc.model.RpcResponse;
import com.samoyer.rpc.model.ServiceMetaInfo;
import com.samoyer.rpc.protocol.*;
import com.samoyer.rpc.registry.Registry;
import com.samoyer.rpc.registry.RegistryFactory;
import com.samoyer.rpc.serializer.Serializer;
import com.samoyer.rpc.serializer.SerializerFactory;
import com.samoyer.rpc.server.tcp.VertxTcpClient;
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
    private RandomLoadBalancer randomLoadBalancer;

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

            // 使用随机负载均衡器获取服务
//            ServiceMetaInfo selectedServiceMetaInfo = serviceMetaInfoList.get(0);
            ServiceMetaInfo selectedServiceMetaInfo = randomLoadBalancer.select(serviceMetaInfoList);

            //发送TCP请求（发送请求的操作封装在VertxTcpClient.doRequest）
            RpcResponse rpcResponse = VertxTcpClient.doRequest(rpcRequest, selectedServiceMetaInfo);

            return rpcResponse.getData();
        } catch (Exception e) {
            log.error("调用失败");
        }

        return null;
    }
}
