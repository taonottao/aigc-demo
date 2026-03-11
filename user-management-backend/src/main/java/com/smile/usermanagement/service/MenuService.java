package com.smile.usermanagement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smile.usermanagement.entity.Menu;
import com.smile.usermanagement.web.MenuNode;
import java.util.List;

public interface MenuService extends IService<Menu> {

    List<MenuNode> getMenuTree();

    Menu createMenu(Menu menu);

    Menu updateMenu(Long id, Menu menu);

    void deleteMenu(Long id);
}

