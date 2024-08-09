package com.samoyer.rpc.proxy;

import com.samoyer.rpc.RpcApplication;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Proxy;

/**
 * 服务代理工厂（用于创建代理对象）
 */
@Slf4j
public class ServiceProxyFactory {

    /**
     * 根据服务类获取代理对象
     * @param serviceClass
     * @return
     * @param <T>
     */
    public static <T> T getProxy(Class<T> serviceClass){
        //根据配置中mock的值来区分创建哪种代理对象，如果设置mock则创建一个其方法返回值固定自设的代理对象
        if (RpcApplication.getRpcConfig().isMock()){
            log.info("获取Mock代理对象");
            return getMockProxy(serviceClass);
        }

        //通过Proxy.newProxyInstance方法为指定类型serviceClass创建代理对象
        log.info("获取Service代理对象");
        return (T) Proxy.newProxyInstance(
                serviceClass.getClassLoader(),
                new Class[]{serviceClass},
                new ServiceProxy()
        );
    }

    /**
     * 根据服务类获取Mock代理对象
     * @param serviceClass
     * @return
     * @param <T>
     */
    public static <T> T getMockProxy(Class<T> serviceClass){
        return (T) Proxy.newProxyInstance(
                serviceClass.getClassLoader(),
                new Class[]{serviceClass},
                new MockServiceProxy()
        );
    }
}
