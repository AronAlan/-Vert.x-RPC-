package com.samoyer.consumer.run;

import com.samoyer.common.domain.po.User;
import com.samoyer.common.service.UserService;
import com.samoyer.consumer.proxy.ServiceProxyFactory;
import com.samoyer.consumer.proxy.UserServiceProxy;

/**
 * 简易的服务消费者
 * 目标：通过RPC框架，得到一个可以调用provider的代理对象
 * 像调用本地方法一样调用UserService的方法
 */
public class EasyConsumer {
    public static void main(String[] args) {
        User user=new User();
        user.setName("samoyer");

        //调用
//        UserService userService=new UserServiceProxy();//使用静态代理
        UserService userService= ServiceProxyFactory.getProxy(UserService.class);
        User newUser=userService.getUserInfo(user);
        if (newUser!=null){
            System.out.println("newUser:"+newUser.getName());
        }else {
            System.out.println("newUser = null");
        }

    }
}
