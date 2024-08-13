package com.samoyer.rpc.registry;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.json.JSONUtil;
import com.samoyer.rpc.config.RegistryConfig;
import com.samoyer.rpc.model.ServiceMetaInfo;
import io.etcd.jetcd.*;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.watch.WatchEvent;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Etcd注册中心实现类
 *
 * @author Samoyer
 * @since 2024-08-12
 */
@Slf4j
public class EtcdRegistry implements Registry {

    private Client client;
    private KV kvClient;
    /**
     * 根节点
     */
    private static final String ETCD_ROOT_PATH = "/rpc/";
    /**
     * 本机注册的节点Key集合（用于维护续期）
     */
    private final Set<String> localRegisterNodeKeySet=new HashSet<>();
    /**
     * 注册中心服务缓存
     */
    private final RegistryServiceCache registryServiceCache=new RegistryServiceCache();
    /**
     * 正在监听的key集合
     * 使用ConcurrentHashSet防止并发冲突
     */
    private final Set<String> watchingKeySet=new ConcurrentHashSet<>();


    /**
     * 初始化
     *
     * @param registryConfig
     */
    @Override
    public void init(RegistryConfig registryConfig) {
        client = Client.builder()
                .endpoints(registryConfig.getAddress())
                .connectTimeout(Duration.ofMillis(registryConfig.getTimeout()))
                .build();
        kvClient = client.getKVClient();
        //调用心跳检测机制
        heartBeat();
    }

    /**
     * 注册服务（服务端）
     *
     * @param serviceMetaInfo
     * @throws Exception
     */
    @Override
    public void register(ServiceMetaInfo serviceMetaInfo) throws Exception {
        //创建Lease(租约/过期时间)和KV客户端
        Lease leaseClient = client.getLeaseClient();

        //创建租约（s)
        long leaseId = leaseClient.grant(30).get().getID();

        //设置要存储的键值对(serviceName:serviceVersion:serviceHost:servicePort)
        String registerKey = ETCD_ROOT_PATH + serviceMetaInfo.getServiceNodeKey();
        ByteSequence key = ByteSequence.from(registerKey, StandardCharsets.UTF_8);
        ByteSequence value = ByteSequence.from(JSONUtil.toJsonStr(serviceMetaInfo), StandardCharsets.UTF_8);

        //将键值对与租约关联起来，并设置过期时间
        PutOption putOption = PutOption.builder()
                .withLeaseId(leaseId)
                .build();
        kvClient.put(key, value, putOption).get();

        //在服务注册时，添加节点到本机注册的key集合中，用于心跳检测
        localRegisterNodeKeySet.add(registerKey);

    }

    /**
     * 注销服务（服务端）
     *
     * @param serviceMetaInfo
     */
    @Override
    public void unRegister(ServiceMetaInfo serviceMetaInfo) {
        String registerKey=ETCD_ROOT_PATH + serviceMetaInfo.getServiceNodeKey();
        kvClient.delete(ByteSequence.from(registerKey, StandardCharsets.UTF_8));
        //服务注销时，从本地缓存集合中移除相应的节点
        localRegisterNodeKeySet.remove(registerKey);
    }

    /**
     * 服务发现（获取某服务的所有节点，消费端）
     *
     * @param serviceKey
     * @return
     */
    @Override
    public List<ServiceMetaInfo> serviceDiscovery(String serviceKey) {
        //优先从缓存中读取服务
        List<ServiceMetaInfo> cachedServiceMetaInfoList = registryServiceCache.readCache();
        if (cachedServiceMetaInfoList!=null){
            log.info("从缓存中加载服务列表");
            return cachedServiceMetaInfoList;
        }

        //缓存中不存在再去注册中心获取

        //根据服务名称作为前缀，从Etcd获取节点列表
        //前缀搜索
        log.info("从注册中心加载服务列表");
        String searchPrefix = ETCD_ROOT_PATH + serviceKey;

        try {
            //前缀查询
            GetOption getOption = GetOption.builder().isPrefix(true).build();
            List<KeyValue> keyValues = kvClient.get(ByteSequence.from(searchPrefix, StandardCharsets.UTF_8), getOption)
                    .get()
                    .getKvs();

            //解析服务信息
            List<ServiceMetaInfo> serviceMetaInfoList = keyValues.stream()
                    .map(keyValue -> {
                        //获取服务同时开启对每个节点的key的监听
                        String key = keyValue.getKey().toString(StandardCharsets.UTF_8);
                        watch(key);

                        //获取keyValue的value
                        String value = keyValue.getValue().toString(StandardCharsets.UTF_8);
                        return JSONUtil.toBean(value, ServiceMetaInfo.class);
                    })
                    .collect(Collectors.toList());

            //从注册中心读取到了服务信息，写入缓存
            registryServiceCache.writeCache(serviceMetaInfoList);

            return serviceMetaInfoList;
        }catch (Exception e){
            throw new RuntimeException("获取服务列表失败",e);
        }
    }

    /**
     * 服务销毁
     */
    @Override
    public void destroy() {
        log.info("当前节点下线");
        //下线节点
        //遍历本节点所有的key
        for (String key : localRegisterNodeKeySet) {
            try {
                kvClient.delete(ByteSequence.from(key,StandardCharsets.UTF_8)).get();
            } catch (Exception e) {
                throw new RuntimeException(key+"节点下线失败",e);
            }
        }

        //释放资源
        if (kvClient!=null){
            kvClient.close();
        }
        if (client!=null){
            client.close();
        }
    }

    /**
     * 心跳检测
     */
    @Override
    public void heartBeat() {
        //10秒续签一次
        CronUtil.schedule("*/10 * * * * *", new Task() {
            @Override
            public void execute() {
                //遍历本节点所有的key
                for (String key : localRegisterNodeKeySet) {
                    try {
                        List<KeyValue> keyValues = kvClient.get(ByteSequence.from(key, StandardCharsets.UTF_8))
                                .get()
                                .getKvs();

                        //列表为空：该节点已经过期（需要重启节点才能重新注册）
                        if (CollUtil.isEmpty(keyValues)){
                            continue;
                        }

                        //不为空：节点未过期，进行续签(重新执行一遍注册操作)
                        KeyValue keyValue = keyValues.get(0);
                        String value = keyValue.getValue().toString(StandardCharsets.UTF_8);
                        ServiceMetaInfo serviceMetaInfo = JSONUtil.toBean(value, ServiceMetaInfo.class);
                        register(serviceMetaInfo);

                    }catch (Exception e){
                        throw new RuntimeException(key+" 续签失败",e);
                    }
                }
            }
        });
        //设置Hutool的CronUtil支持秒级别的定时任务
        CronUtil.setMatchSecond(true);
        CronUtil.start();
    }

    /**
     * 监听（消费端）
     * @param serviceNodeKey
     */
    @Override
    public void watch(String serviceNodeKey) {
        Watch watchClient = client.getWatchClient();
        //之前未被监听，开启监听
        boolean newWatch = watchingKeySet.add(serviceNodeKey);
        if (newWatch){
            watchClient.watch(ByteSequence.from(serviceNodeKey,StandardCharsets.UTF_8),response -> {
                for (WatchEvent event : response.getEvents()) {
                    switch (event.getEventType()){
                        //key删除时触发
                        case DELETE:
                            //清理注册服务缓存
                            registryServiceCache.clearCache();
                            break;
                        case PUT:
                        default:
                            break;
                    }
                }
            });
        }
    }

}
