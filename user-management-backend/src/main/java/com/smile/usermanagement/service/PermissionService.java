package com.smile.usermanagement.service;

import com.smile.usermanagement.entity.Menu;
import java.util.List;
import java.util.Set;

public interface PermissionService {

    Set<String> loadAuthorities(Long userId);

    List<Menu> listUserMenus(Long userId);

    List<String> listUserPermissions(Long userId);

    void evictUserCache(Long userId);

    void evictAllCache();
}
