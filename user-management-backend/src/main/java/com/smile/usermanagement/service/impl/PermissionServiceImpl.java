package com.smile.usermanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smile.usermanagement.entity.Menu;
import com.smile.usermanagement.entity.Role;
import com.smile.usermanagement.mapper.MenuMapper;
import com.smile.usermanagement.mapper.RoleMapper;
import com.smile.usermanagement.mapper.RoleMenuMapper;
import com.smile.usermanagement.mapper.UserRoleMapper;
import com.smile.usermanagement.service.PermissionService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PermissionServiceImpl implements PermissionService {

    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final MenuMapper menuMapper;

    public PermissionServiceImpl(UserRoleMapper userRoleMapper, RoleMapper roleMapper, RoleMenuMapper roleMenuMapper,
                                 MenuMapper menuMapper) {
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.menuMapper = menuMapper;
    }

    @Override
    public Set<String> loadAuthorities(Long userId) {
        List<Role> roles = listEnabledRoles(userId);
        List<Menu> menus = listEnabledMenus(roles);

        Set<String> authorities = new HashSet<>();
        for (Role role : roles) {
            authorities.add("ROLE_" + role.getCode());
        }
        for (Menu menu : menus) {
            if (StringUtils.hasText(menu.getPermCode())) {
                authorities.add(menu.getPermCode());
            }
        }
        return authorities;
    }

    @Override
    public List<Menu> listUserMenus(Long userId) {
        List<Role> roles = listEnabledRoles(userId);
        List<Menu> menus = listEnabledMenus(roles);
        return menus.stream()
            .filter(m -> "MENU".equalsIgnoreCase(m.getType()))
            .sorted((a, b) -> {
                int sa = a.getSortNo() == null ? 0 : a.getSortNo();
                int sb = b.getSortNo() == null ? 0 : b.getSortNo();
                if (sa != sb) {
                    return Integer.compare(sa, sb);
                }
                return Long.compare(a.getId(), b.getId());
            })
            .toList();
    }

    @Override
    public List<String> listUserPermissions(Long userId) {
        List<Role> roles = listEnabledRoles(userId);
        List<Menu> menus = listEnabledMenus(roles);
        return menus.stream()
            .map(Menu::getPermCode)
            .filter(StringUtils::hasText)
            .distinct()
            .sorted()
            .toList();
    }

    private List<Role> listEnabledRoles(Long userId) {
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectList(new LambdaQueryWrapper<Role>()
            .in(Role::getId, roleIds)
            .eq(Role::getStatus, 1));
    }

    private List<Menu> listEnabledMenus(List<Role> roles) {
        if (roles.isEmpty()) {
            return List.of();
        }

        Set<Long> menuIds = new HashSet<>();
        for (Role role : roles) {
            List<Long> ids = roleMenuMapper.selectMenuIdsByRoleId(role.getId());
            if (ids != null) {
                menuIds.addAll(ids);
            }
        }
        if (menuIds.isEmpty()) {
            return List.of();
        }

        return menuMapper.selectList(new LambdaQueryWrapper<Menu>()
            .in(Menu::getId, menuIds)
            .eq(Menu::getStatus, 1));
    }
}

