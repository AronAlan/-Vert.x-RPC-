package com.samoyer.rpc.loadbalancer;

import com.samoyer.rpc.model.ServiceMetaInfo;

import java.util.List;
import java.util.Random;

/**
 * 随机负载均衡器
 * @author Samoyer
 * @since 2024-08-16
 */
public class RandomLoadBalancer implements LoadBalancer{
    private final Random random=new Random();

    /**
     * 随机负载
     * @param serviceMetaInfoList 可用服务列表
     * @return ServiceMetaInfo
     */

    @Override
    public ServiceMetaInfo select(List<ServiceMetaInfo> serviceMetaInfoList) {
        int size = serviceMetaInfoList.size();
        if (size==0){
            return null;
        }

        //只有一个服务，不需要随机
        if (size==1){
            return serviceMetaInfoList.get(0);
        }

        return serviceMetaInfoList.get(random.nextInt(size));
    }
}
