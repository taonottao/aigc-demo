package com.smile.usermanagement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smile.usermanagement.entity.Role;
import java.util.List;

public interface RoleService extends IService<Role> {

    Role createRole(Role role);

    Role updateRole(Long id, Role role);

    void replaceRoleMenus(Long roleId, List<Long> menuIds);

    List<Long> listRoleMenuIds(Long roleId);
}

