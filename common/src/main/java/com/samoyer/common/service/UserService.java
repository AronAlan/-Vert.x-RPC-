package com.samoyer.common.service;

import com.samoyer.common.domain.po.User;

/**
 * 用户服务接口
 */
public interface UserService {
    /**
     * 获取用户
     */
    User getUserInfo(User user);
}
