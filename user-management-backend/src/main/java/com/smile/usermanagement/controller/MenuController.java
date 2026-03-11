package com.smile.usermanagement.controller;

import com.smile.usermanagement.entity.Menu;
import com.smile.usermanagement.security.SecurityUtils;
import com.smile.usermanagement.security.UserPrincipal;
import com.smile.usermanagement.service.AuditService;
import com.smile.usermanagement.service.MenuService;
import com.smile.usermanagement.web.MenuNode;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/menus")
public class MenuController {

    private final MenuService menuService;
    private final AuditService auditService;

    public MenuController(MenuService menuService, AuditService auditService) {
        this.menuService = menuService;
        this.auditService = auditService;
    }

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('role:view')")
    public List<MenuNode> tree() {
        return menuService.getMenuTree();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('perm:save')")
    @ResponseStatus(HttpStatus.CREATED)
    public Menu create(@Valid @RequestBody Menu menu) {
        Menu created = menuService.createMenu(menu);
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        if (principal != null) {
            auditService.opLog(principal.id(), principal.username(), "MENU", "CREATE", "menuId=" + created.getId());
        }
        return created;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('perm:save')")
    public Menu update(@PathVariable Long id, @Valid @RequestBody Menu menu) {
        Menu updated = menuService.updateMenu(id, menu);
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        if (principal != null) {
            auditService.opLog(principal.id(), principal.username(), "MENU", "UPDATE", "menuId=" + updated.getId());
        }
        return updated;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('perm:save')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        menuService.deleteMenu(id);
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        if (principal != null) {
            auditService.opLog(principal.id(), principal.username(), "MENU", "DELETE", "menuId=" + id);
        }
    }
}

