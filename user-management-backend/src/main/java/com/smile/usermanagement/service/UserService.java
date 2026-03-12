package com.smile.usermanagement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smile.usermanagement.entity.User;
import java.util.List;

public interface UserService extends IService<User> {

    List<User> listByOrgAndKeyword(Long orgId, String keyword);

    IPage<User> pageByOrgAndKeyword(Long orgId, String keyword, long page, long size);

    User createUser(User user, List<Long> roleIds);

    User updateUser(Long id, User user, List<Long> roleIds);

    void resetPassword(Long id, String newPassword);

    List<Long> listRoleIds(Long userId);

    void replaceRoles(Long userId, List<Long> roleIds);

    boolean deleteUserById(Long userId);
}
