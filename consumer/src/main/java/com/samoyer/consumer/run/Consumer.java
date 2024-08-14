package com.samoyer.consumer.run;

import com.samoyer.common.model.po.User;
import com.samoyer.common.service.UserService;
import com.samoyer.rpc.proxy.ServiceProxyFactory;

/**
 * 服务消费者
 *
 * @author Samoyer
 */
public class Consumer{
    public static void main(String[] args) throws InterruptedException {
//        RpcConfig rpc = ConfigUtils.loadConfig(RpcConfig.class, RpcConstant.DEFAULT_CONFIG_PREFIX); //普通加载配置
//        RpcConfig rpcConfig = RpcApplication.getRpcConfig(); //全局配置对象

        User user=new User();
        user.setName("samoyer");

        UserService userService= ServiceProxyFactory.getProxy(UserService.class);
        User newUser = userService.getUserInfo(user);
        if (newUser!=null){
            System.out.println("newUser:"+newUser.getName());
        }else {
            System.out.println("newUser = null");
        }


        /*
        //模拟多次消费者调用，测试是否经过缓存
        Thread.sleep(3000);

        User user2=new User();
        user2.setName("samoyer2");
        UserService userService2= ServiceProxyFactory.getProxy(UserService.class);
        User newUser2 = userService2.getUserInfo(user2);
        if (newUser2!=null){
            System.out.println("newUser2:"+newUser2.getName());
        }else {
            System.out.println("newUser2 = null");
        }

        Thread.sleep(3000);
        User user3=new User();
        user3.setName("samoyer3");
        UserService userService3= ServiceProxyFactory.getProxy(UserService.class);
        User newUser3 = userService3.getUserInfo(user3);
        if (newUser3!=null){
            System.out.println("newUser3:"+newUser3.getName());
        }else {
            System.out.println("newUser3 = null");
        }
         */


        /*
        //测试mock
        Integer number = userService.getNumber();
        System.out.println("number="+number);
         */
    }
}
