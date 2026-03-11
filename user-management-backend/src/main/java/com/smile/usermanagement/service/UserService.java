package com.smile.usermanagement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smile.usermanagement.entity.User;
import java.util.List;

public interface UserService extends IService<User> {

    List<User> listByOrgAndKeyword(Long orgId, String keyword);

    User createUser(User user, List<Long> roleIds);

    User updateUser(Long id, User user, List<Long> roleIds);

    void resetPassword(Long id, String newPassword);

    List<Long> listRoleIds(Long userId);

    void replaceRoles(Long userId, List<Long> roleIds);
}
