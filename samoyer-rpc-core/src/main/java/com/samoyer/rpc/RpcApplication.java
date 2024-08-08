package com.samoyer.rpc;

import com.samoyer.rpc.config.RpcConfig;
import com.samoyer.rpc.constant.RpcConstant;
import com.samoyer.rpc.utils.ConfigUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * RPC框架应用
 * 存放项目全局用到的变量
 * 双检锁单例模式实现
 */
@Slf4j
public class RpcApplication {
    private static volatile RpcConfig rpcConfig;

    /**
     * 框架初始化，支持传入自定义配置
     * @param newRpcConfig
     */
    public static void init(RpcConfig newRpcConfig){
        rpcConfig=newRpcConfig;
        log.info("RPC init, config={}",newRpcConfig.toString());
    }

    /**
     * 初始化
     */
    public static void init(){
        RpcConfig newRpcConfig;
        try {
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
