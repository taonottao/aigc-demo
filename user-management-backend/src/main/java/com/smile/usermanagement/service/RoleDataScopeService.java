package com.smile.usermanagement.service;

import com.smile.usermanagement.entity.RoleDataScope;
import java.util.List;

public interface RoleDataScopeService {

    List<RoleDataScope> listByRoleId(Long roleId);

    void replaceByRoleId(Long roleId, List<RoleDataScope> scopes);
}

