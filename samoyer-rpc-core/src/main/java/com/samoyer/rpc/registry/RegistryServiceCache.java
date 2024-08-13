package com.samoyer.rpc.registry;

import com.samoyer.rpc.model.ServiceMetaInfo;

import java.util.List;

/**
 * 注册中心服务的本地缓存
 * @author Samoyer
 * @since 2024-08-13
 */
public class RegistryServiceCache {
    /**
     * 服务缓存
     */
    List<ServiceMetaInfo> serviceCache;

    /**
     * 写缓存
     * @param newServiceCache
     */
    void writeCache(List<ServiceMetaInfo> newServiceCache){
        this.serviceCache=newServiceCache;
    }

    /**
     * 读缓存
     * @return
     */
    List<ServiceMetaInfo> readCache(){
        return this.serviceCache;
    }

    /**
     * 清空缓存
     */
    void clearCache(){
        this.serviceCache=null;
    }
}
