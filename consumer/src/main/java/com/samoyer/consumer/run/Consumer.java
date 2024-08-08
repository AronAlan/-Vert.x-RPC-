package com.samoyer.consumer.run;

import com.samoyer.common.model.po.User;
import com.samoyer.common.service.UserService;
import com.samoyer.rpc.proxy.ServiceProxyFactory;

/**
 * 服务消费者
 */
public class Consumer{
    public static void main(String[] args) {
//        RpcConfig rpc = ConfigUtils.loadConfig(RpcConfig.class, RpcConstant.DEFAULT_CONFIG_PREFIX); //普通加载配置
//        RpcConfig rpcConfig = RpcApplication.getRpcConfig(); //全局配置对象
//        System.out.println(rpcConfig);

        User user=new User();
        user.setName("samoyer");

        UserService userService= ServiceProxyFactory.getProxy(UserService.class);
        User newUser = userService.getUserInfo(user);
        if (newUser!=null){
            System.out.println("newUser:"+newUser.getName());
        }else {
            System.out.println("newUser = null");
        }

        short number = userService.getNumber();
        System.out.println(number);
    }
}
