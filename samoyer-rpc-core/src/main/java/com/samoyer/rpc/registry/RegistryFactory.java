package com.samoyer.rpc.registry;

import com.samoyer.rpc.spi.SpiLoader;

/**
 * 注册中心工厂（用于获取注册中心对象）
 * @author Samoyer
 * @since 2024-08-12
 */
public class RegistryFactory {
    static {
        SpiLoader.load(Registry.class);
    }

    /**
     * 默认注册中心 Etcd
     */
    private static final Registry DEFAULT_REGISTRY=new EtcdRegistry();

    /**
     * 获取实例
     */
    public static Registry getInstance(String key){
        return SpiLoader.getInstance(Registry.class,key);
    }
}
