package com.smile.usermanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smile.usermanagement.entity.RoleDataScope;
import com.smile.usermanagement.entity.User;
import com.smile.usermanagement.mapper.RoleDataScopeMapper;
import com.smile.usermanagement.mapper.UserMapper;
import com.smile.usermanagement.mapper.UserRoleMapper;
import com.smile.usermanagement.service.DataScopeService;
import com.smile.usermanagement.service.OrgService;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class DataScopeServiceImpl implements DataScopeService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleDataScopeMapper roleDataScopeMapper;
    private final OrgService orgService;

    public DataScopeServiceImpl(UserMapper userMapper, UserRoleMapper userRoleMapper,
                                RoleDataScopeMapper roleDataScopeMapper, OrgService orgService) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleDataScopeMapper = roleDataScopeMapper;
        this.orgService = orgService;
    }

    @Override
    public Scope resolveUserModuleScope(Long userId) {
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return Scope.SELF_ONLY;
        }
        List<RoleDataScope> scopes = roleDataScopeMapper.selectList(new LambdaQueryWrapper<RoleDataScope>()
            .in(RoleDataScope::getRoleId, roleIds)
            .eq(RoleDataScope::getModuleCode, MODULE_USER));
        if (scopes.isEmpty()) {
            return Scope.SELF_ONLY;
        }
        return scopes.stream()
            .map(RoleDataScope::getScope)
            .map(this::safeScope)
            .max(Comparator.comparingInt(this::rank))
            .orElse(Scope.SELF_ONLY);
    }

    @Override
    public Set<Long> resolveAccessibleOrgIds(Long userId) {
        Scope scope = resolveUserModuleScope(userId);
        return resolveAccessibleOrgIds(userId, scope);
    }

    @Override
    public Set<Long> resolveAccessibleOrgIds(Long userId, Scope scope) {
        if (scope == Scope.ALL) {
            return Set.of();
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Set.of();
        }
        Long orgId = user.getOrgId();
        if (orgId == null) {
            return Set.of();
        }
        if (scope == Scope.ORG_ONLY) {
            return Set.of(orgId);
        }
        if (scope == Scope.ORG_AND_CHILDREN) {
            return orgService.getDescendantOrgIds(orgId);
        }
        return Set.of();
    }

    private Scope safeScope(String value) {
        try {
            return Scope.valueOf(value);
        } catch (Exception ex) {
            return Scope.SELF_ONLY;
        }
    }

    private int rank(Scope scope) {
        return switch (scope) {
            case ALL -> 4;
            case ORG_AND_CHILDREN -> 3;
            case ORG_ONLY -> 2;
            case SELF_ONLY -> 1;
        };
    }
}
