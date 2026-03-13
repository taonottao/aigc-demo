package com.smile.usermanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smile.usermanagement.entity.Menu;
import com.smile.usermanagement.entity.Role;
import com.smile.usermanagement.mapper.MenuMapper;
import com.smile.usermanagement.mapper.RoleMapper;
import com.smile.usermanagement.mapper.RoleMenuMapper;
import com.smile.usermanagement.mapper.UserRoleMapper;
import com.smile.usermanagement.service.PermissionService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PermissionServiceImpl implements PermissionService {
    private static final long CACHE_TTL_MILLIS = 60_000L;

    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final MenuMapper menuMapper;
    private final ConcurrentMap<Long, CacheItem> cache = new ConcurrentHashMap<>();

    public PermissionServiceImpl(UserRoleMapper userRoleMapper, RoleMapper roleMapper, RoleMenuMapper roleMenuMapper,
                                 MenuMapper menuMapper) {
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.menuMapper = menuMapper;
    }

    @Override
    public Set<String> loadAuthorities(Long userId) {
        return getPermissionBundle(userId).authorities();
    }

    @Override
    public List<Menu> listUserMenus(Long userId) {
        return getPermissionBundle(userId).menus();
    }

    @Override
    public List<String> listUserPermissions(Long userId) {
        return getPermissionBundle(userId).permissions();
    }

    @Override
    public void evictUserCache(Long userId) {
        if (userId != null) {
            cache.remove(userId);
        }
    }

    @Override
    public void evictAllCache() {
        cache.clear();
    }

    private PermissionBundle getPermissionBundle(Long userId) {
        if (userId == null) {
            return PermissionBundle.empty();
        }
        CacheItem item = cache.get(userId);
        long now = System.currentTimeMillis();
        if (item != null && item.expiresAtMillis() > now) {
            return item.bundle();
        }
        PermissionBundle refreshed = loadPermissionBundle(userId);
        cache.put(userId, new CacheItem(refreshed, now + CACHE_TTL_MILLIS));
        return refreshed;
    }

    private PermissionBundle loadPermissionBundle(Long userId) {
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

        List<Menu> sortedMenus = menus.stream()
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

        List<String> permissions = menus.stream()
            .map(Menu::getPermCode)
            .filter(StringUtils::hasText)
            .distinct()
            .sorted()
            .toList();

        return new PermissionBundle(
            Set.copyOf(authorities),
            List.copyOf(sortedMenus),
            List.copyOf(permissions)
        );
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

        List<Long> roleIds = roles.stream().map(Role::getId).toList();
        List<Long> menuIds = roleMenuMapper.selectMenuIdsByRoleIds(roleIds);
        if (menuIds.isEmpty()) {
            return List.of();
        }

        return menuMapper.selectList(new LambdaQueryWrapper<Menu>()
            .in(Menu::getId, menuIds)
            .eq(Menu::getStatus, 1));
    }

    private record PermissionBundle(Set<String> authorities, List<Menu> menus, List<String> permissions) {
        private static PermissionBundle empty() {
            return new PermissionBundle(Set.of(), List.of(), List.of());
        }
    }

    private record CacheItem(PermissionBundle bundle, long expiresAtMillis) {}
}
