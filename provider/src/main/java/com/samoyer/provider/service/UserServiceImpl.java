package com.samoyer.provider.service;

import com.samoyer.common.model.po.User;
import com.samoyer.common.service.UserService;

/**
 * 用户服务实现类
 */
public class UserServiceImpl implements UserService {
    @Override
    public User getUserInfo(User user) {
        System.out.println("用户名："+user.getName());
        return user;
    }
}
