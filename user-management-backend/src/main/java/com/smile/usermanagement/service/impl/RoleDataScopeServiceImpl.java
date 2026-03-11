package com.smile.usermanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smile.usermanagement.entity.RoleDataScope;
import com.smile.usermanagement.mapper.RoleDataScopeMapper;
import com.smile.usermanagement.service.RoleDataScopeService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RoleDataScopeServiceImpl implements RoleDataScopeService {

    private final RoleDataScopeMapper roleDataScopeMapper;

    public RoleDataScopeServiceImpl(RoleDataScopeMapper roleDataScopeMapper) {
        this.roleDataScopeMapper = roleDataScopeMapper;
    }

    @Override
    public List<RoleDataScope> listByRoleId(Long roleId) {
        return roleDataScopeMapper.selectList(new LambdaQueryWrapper<RoleDataScope>()
            .eq(RoleDataScope::getRoleId, roleId));
    }

    @Override
    public void replaceByRoleId(Long roleId, List<RoleDataScope> scopes) {
        roleDataScopeMapper.delete(new LambdaQueryWrapper<RoleDataScope>().eq(RoleDataScope::getRoleId, roleId));
        if (scopes == null) {
            return;
        }
        for (RoleDataScope scope : scopes) {
            if (scope == null || !StringUtils.hasText(scope.getModuleCode()) || !StringUtils.hasText(scope.getScope())) {
                continue;
            }
            RoleDataScope row = new RoleDataScope();
            row.setRoleId(roleId);
            row.setModuleCode(scope.getModuleCode().trim().toUpperCase());
            row.setScope(scope.getScope().trim().toUpperCase());
            row.setCreatedAt(LocalDateTime.now());
            row.setUpdatedAt(LocalDateTime.now());
            roleDataScopeMapper.insert(row);
        }
    }
}

