package com.samoyer.rpc.loadbalancer;

import com.samoyer.rpc.model.ServiceMetaInfo;

import java.util.List;

/**
 * 负载均衡器（消费端使用）
 * @author Samoyer
 * @since 2024-08-16
 */
public interface LoadBalancer {
    /**
     * 选择服务调用
     * @param serviceMetaInfoList 可用服务列表
     * @return
     */
    ServiceMetaInfo select(List<ServiceMetaInfo> serviceMetaInfoList);
}
