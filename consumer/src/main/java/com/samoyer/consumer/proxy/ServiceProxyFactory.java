package com.samoyer.consumer.proxy;

import java.lang.reflect.Proxy;

/**
 * 服务代理工厂（用于创建代理对象）
 */
public class ServiceProxyFactory {
    public static <T> T getProxy(Class<T> serviceClass){
        //通过Proxy.newProxyInstance方法为指定类型serviceClass创建代理对象
        return (T) Proxy.newProxyInstance(
                serviceClass.getClassLoader(),
                new Class[]{serviceClass},
                new ServiceProxy()
        );
    }
}
