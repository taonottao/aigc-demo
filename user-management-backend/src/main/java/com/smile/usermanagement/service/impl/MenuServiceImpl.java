package com.smile.usermanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smile.usermanagement.entity.Menu;
import com.smile.usermanagement.mapper.MenuMapper;
import com.smile.usermanagement.service.MenuService;
import com.smile.usermanagement.web.MenuNode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {

    @Override
    public List<MenuNode> getMenuTree() {
        List<Menu> menus = list(new LambdaQueryWrapper<Menu>()
            .orderByAsc(Menu::getSortNo)
            .orderByAsc(Menu::getId));

        Map<Long, MenuNode> map = new HashMap<>();
        for (Menu menu : menus) {
            map.put(menu.getId(), MenuNode.from(menu));
        }

        List<MenuNode> roots = new ArrayList<>();
        for (Menu menu : menus) {
            MenuNode node = map.get(menu.getId());
            Long parentId = menu.getParentId() == null ? 0L : menu.getParentId();
            if (parentId == 0L || !map.containsKey(parentId)) {
                roots.add(node);
                continue;
            }
            map.get(parentId).getChildren().add(node);
        }
        sortTree(roots);
        return roots;
    }

    @Override
    public Menu createMenu(Menu menu) {
        if (!StringUtils.hasText(menu.getName())) {
            throw new IllegalArgumentException("Menu name is required");
        }
        if (!StringUtils.hasText(menu.getType())) {
            menu.setType("MENU");
        }
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        if (menu.getSortNo() == null) {
            menu.setSortNo(0);
        }
        if (menu.getStatus() == null) {
            menu.setStatus(1);
        }
        LocalDateTime now = LocalDateTime.now();
        menu.setCreatedAt(now);
        menu.setUpdatedAt(now);
        save(menu);
        return menu;
    }

    @Override
    public Menu updateMenu(Long id, Menu menu) {
        Menu existing = getById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Menu not found: " + id);
        }
        existing.setParentId(menu.getParentId() == null ? 0L : menu.getParentId());
        existing.setName(menu.getName());
        existing.setPath(menu.getPath());
        existing.setIcon(menu.getIcon());
        existing.setPermCode(menu.getPermCode());
        existing.setType(menu.getType());
        existing.setSortNo(menu.getSortNo());
        existing.setStatus(menu.getStatus());
        existing.setUpdatedAt(LocalDateTime.now());
        updateById(existing);
        return existing;
    }

    @Override
    public void deleteMenu(Long id) {
        long childrenCount = count(new LambdaQueryWrapper<Menu>().eq(Menu::getParentId, id));
        if (childrenCount > 0) {
            throw new IllegalArgumentException("Menu has children; delete children first");
        }
        boolean removed = removeById(id);
        if (!removed) {
            throw new IllegalArgumentException("Menu not found: " + id);
        }
    }

    private void sortTree(List<MenuNode> nodes) {
        nodes.sort(Comparator.comparingInt(n -> n.getSortNo() == null ? 0 : n.getSortNo()));
        for (MenuNode node : nodes) {
            sortTree(node.getChildren());
        }
    }
}

