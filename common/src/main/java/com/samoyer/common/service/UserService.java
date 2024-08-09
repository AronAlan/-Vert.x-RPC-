package com.samoyer.common.service;

import com.samoyer.common.model.po.User;

/**
 * 用户服务接口
 */
public interface UserService {
    /**
     * 获取用户
     * @param user
     * @return
     */
    User getUserInfo(User user);

    /**
     * 获取数字
     * mock
     * default表示允许在接口中定义有具体实现的方法，不要求实现类必须重写
     * @return
     */
    default Integer getNumber(){
        return 1;
    }
}
