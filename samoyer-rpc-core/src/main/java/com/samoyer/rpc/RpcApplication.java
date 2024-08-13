package com.samoyer.rpc;

import com.samoyer.rpc.config.RegistryConfig;
import com.samoyer.rpc.config.RpcConfig;
import com.samoyer.rpc.constant.RpcConstant;
import com.samoyer.rpc.registry.Registry;
import com.samoyer.rpc.registry.RegistryFactory;
import com.samoyer.rpc.utils.ConfigUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * RPC框架应用
 * 存放项目全局用到的变量
 * 双检锁单例模式实现
 */
@Slf4j
public class RpcApplication {
    /**
     * volatile 不同线程都能共享
     */
    private static volatile RpcConfig rpcConfig;

    /**
     * 框架初始化，支持传入自定义配置
     * @param newRpcConfig
     */
    public static void init(RpcConfig newRpcConfig){
        rpcConfig=newRpcConfig;
        log.info("RPC初始化, config={}",newRpcConfig.toString());

        //注册中心初始化
        //获取注册中心配置
        RegistryConfig registryConfig = rpcConfig.getRegistryConfig();
        //获取注册中心实例对象
        Registry registry = RegistryFactory.getInstance(registryConfig.getRegistry());
        registry.init(registryConfig);
        log.info("registry init, config={}",registryConfig);

        //创建并注册Shutdown Hook , JVM退出时执行操作
        //Shutdown Hook是JVM提供的一种机制，允许开发者在JVM在即将关闭之前执行一些清理工作，比如释放资源，关闭数据库连接等
        Runtime.getRuntime().addShutdownHook(new Thread(registry::destroy));
    }

    /**
     * 初始化
     */
    public static void init(){
        RpcConfig newRpcConfig;
        try {
            //从application.properties读取RpcConfig相关的属性
            newRpcConfig= ConfigUtils.loadConfig(RpcConfig.class, RpcConstant.DEFAULT_CONFIG_PREFIX);
        } catch (Exception e) {
            //配置加载失败，使用默认值
            newRpcConfig=new RpcConfig();
        }
        init(newRpcConfig);
    }

    /**
     * 获取配置 入口
     * @return
     */
    public static RpcConfig getRpcConfig(){
        if (rpcConfig==null){
            //使用RpcApplication.class对象作为锁的对象
            //在整个RpcApplication类的所有实例之间共享同一个锁
            //此同步块保证在多线程环境下只有一个线程能进入该代码块
            synchronized (RpcApplication.class){
                //再次检查，因为在第一个检查后到获取到锁之前，可能已经有另一个线程完成了初始化
                if (rpcConfig==null){
                    init();
                }
            }
        }
        return rpcConfig;
    }
}
