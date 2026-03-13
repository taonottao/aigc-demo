package com.smile.usermanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smile.usermanagement.entity.Role;
import com.smile.usermanagement.mapper.RoleMapper;
import com.smile.usermanagement.mapper.RoleMenuMapper;
import com.smile.usermanagement.service.PermissionService;
import com.smile.usermanagement.service.RoleService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    private final RoleMenuMapper roleMenuMapper;
    private final PermissionService permissionService;

    public RoleServiceImpl(RoleMenuMapper roleMenuMapper, PermissionService permissionService) {
        this.roleMenuMapper = roleMenuMapper;
        this.permissionService = permissionService;
    }

    @Override
    public Role createRole(Role role) {
        validateRoleCodeUnique(role.getCode(), null);
        if (!StringUtils.hasText(role.getName())) {
            throw new IllegalArgumentException("Role name is required");
        }
        if (role.getStatus() == null) {
            role.setStatus(1);
        }
        LocalDateTime now = LocalDateTime.now();
        role.setCreatedAt(now);
        role.setUpdatedAt(now);
        save(role);
        return role;
    }

    @Override
    public Role updateRole(Long id, Role role) {
        Role existing = getById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Role not found: " + id);
        }
        validateRoleCodeUnique(role.getCode(), id);

        existing.setName(role.getName());
        existing.setCode(role.getCode());
        existing.setDescription(role.getDescription());
        existing.setStatus(role.getStatus());
        existing.setUpdatedAt(LocalDateTime.now());
        updateById(existing);
        permissionService.evictAllCache();
        return existing;
    }

    @Override
    public void replaceRoleMenus(Long roleId, List<Long> menuIds) {
        Role role = getById(roleId);
        if (role == null) {
            throw new IllegalArgumentException("Role not found: " + roleId);
        }
        roleMenuMapper.deleteByRoleId(roleId);
        if (menuIds == null) {
            permissionService.evictAllCache();
            return;
        }
        for (Long menuId : menuIds) {
            if (menuId == null) {
                continue;
            }
            roleMenuMapper.insert(roleId, menuId);
        }
        permissionService.evictAllCache();
    }

    @Override
    public List<Long> listRoleMenuIds(Long roleId) {
        return roleMenuMapper.selectMenuIdsByRoleId(roleId);
    }

    private void validateRoleCodeUnique(String code, Long excludeId) {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("Role code is required");
        }
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<Role>().eq(Role::getCode, code);
        if (excludeId != null) {
            wrapper.ne(Role::getId, excludeId);
        }
        Long count = count(wrapper);
        if (count != null && count > 0) {
            throw new IllegalArgumentException("Role code already exists: " + code);
        }
    }
}
