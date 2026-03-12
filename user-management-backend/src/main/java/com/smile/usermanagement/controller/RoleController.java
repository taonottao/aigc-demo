package com.smile.usermanagement.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smile.usermanagement.entity.Role;
import com.smile.usermanagement.security.SecurityUtils;
import com.smile.usermanagement.security.UserPrincipal;
import com.smile.usermanagement.service.AuditService;
import com.smile.usermanagement.service.RoleDataScopeService;
import com.smile.usermanagement.service.RoleService;
import com.smile.usermanagement.entity.RoleDataScope;
import com.smile.usermanagement.web.PageResult;
import com.smile.usermanagement.web.ReplaceIdsRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;
    private final AuditService auditService;
    private final RoleDataScopeService roleDataScopeService;

    public RoleController(RoleService roleService, AuditService auditService, RoleDataScopeService roleDataScopeService) {
        this.roleService = roleService;
        this.auditService = auditService;
        this.roleDataScopeService = roleDataScopeService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('role:view')")
    public List<Role> list() {
        return roleService.list();
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('role:view')")
    public PageResult<Role> page(@RequestParam(defaultValue = "1") Long page,
                                 @RequestParam(defaultValue = "20") Long size,
                                 @RequestParam(required = false) String keyword) {
        long current = page <= 0 ? 1 : page;
        long pageSize = size <= 0 ? 20 : Math.min(size, 200);
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<Role>()
            .orderByDesc(Role::getId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Role::getName, keyword).or().like(Role::getCode, keyword));
        }
        return PageResult.from(roleService.page(new Page<>(current, pageSize), wrapper));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('role:add')")
    @ResponseStatus(HttpStatus.CREATED)
    public Role create(@Valid @RequestBody Role role) {
        Role created = roleService.createRole(role);
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        if (principal != null) {
            auditService.opLog(principal.id(), principal.username(), "ROLE", "CREATE", "roleId=" + created.getId());
        }
        return created;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('perm:save')")
    public Role update(@PathVariable Long id, @Valid @RequestBody Role role) {
        Role updated = roleService.updateRole(id, role);
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        if (principal != null) {
            auditService.opLog(principal.id(), principal.username(), "ROLE", "UPDATE", "roleId=" + updated.getId());
        }
        return updated;
    }

    @GetMapping("/{id}/menus")
    @PreAuthorize("hasAuthority('role:view')")
    public List<Long> getRoleMenus(@PathVariable Long id) {
        return roleService.listRoleMenuIds(id);
    }

    @PutMapping("/{id}/menus")
    @PreAuthorize("hasAuthority('perm:save')")
    public void replaceRoleMenus(@PathVariable Long id, @RequestBody ReplaceIdsRequest request) {
        roleService.replaceRoleMenus(id, request == null ? null : request.getIds());
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        if (principal != null) {
            auditService.opLog(principal.id(), principal.username(), "ROLE", "BIND_MENUS", "roleId=" + id);
        }
    }

    @GetMapping("/{id}/data-scopes")
    @PreAuthorize("hasAuthority('role:view')")
    public List<RoleDataScope> getDataScopes(@PathVariable Long id) {
        return roleDataScopeService.listByRoleId(id);
    }

    @PutMapping("/{id}/data-scopes")
    @PreAuthorize("hasAuthority('perm:save')")
    public void replaceDataScopes(@PathVariable Long id, @RequestBody List<RoleDataScope> scopes) {
        roleDataScopeService.replaceByRoleId(id, scopes);
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        if (principal != null) {
            auditService.opLog(principal.id(), principal.username(), "ROLE", "DATA_SCOPE", "roleId=" + id);
        }
    }
}
