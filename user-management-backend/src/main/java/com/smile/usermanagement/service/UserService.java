package com.smile.usermanagement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smile.usermanagement.entity.User;
import java.util.List;

public interface UserService extends IService<User> {

    List<User> listByOrgAndKeyword(Long orgId, String keyword);

    User createUser(User user);

    User updateUser(Long id, User user);
}
