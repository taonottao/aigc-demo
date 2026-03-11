package com.smile.usermanagement.controller;

import com.smile.usermanagement.entity.Org;
import com.smile.usermanagement.security.SecurityUtils;
import com.smile.usermanagement.security.UserPrincipal;
import com.smile.usermanagement.service.AuditService;
import com.smile.usermanagement.service.AuthService;
import com.smile.usermanagement.service.OrgService;
import com.smile.usermanagement.service.UserService;
import com.smile.usermanagement.web.OrgNode;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orgs")
public class OrgController {

    private final OrgService orgService;
    private final AuthService authService;
    private final AuditService auditService;
    private final UserService userService;

    public OrgController(OrgService orgService, AuthService authService, AuditService auditService, UserService userService) {
        this.orgService = orgService;
        this.authService = authService;
        this.auditService = auditService;
        this.userService = userService;
    }

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('org:view')")
    public List<OrgNode> tree() {
        return orgService.getOrgTree();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('org:add')")
    @ResponseStatus(HttpStatus.CREATED)
    public Org create(@Valid @RequestBody Org org) {
        Org created = orgService.createOrg(org);
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        if (principal != null) {
            auditService.opLog(principal.id(), principal.username(), "ORG", "CREATE", "orgId=" + created.getId());
        }
        return created;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('org:edit')")
    public Org update(@PathVariable Long id, @Valid @RequestBody Org org) {
        Org updated = orgService.updateOrg(id, org);
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        if (principal != null) {
            auditService.opLog(principal.id(), principal.username(), "ORG", "UPDATE", "orgId=" + updated.getId());
        }
        return updated;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('org:delete')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @RequestHeader(value = "X-Second-Verify", required = false) String token) {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        if (principal != null) {
            authService.assertSecondVerified(principal.id(), token);
        }
        orgService.deleteOrg(id);
        if (principal != null) {
            auditService.opLog(principal.id(), principal.username(), "ORG", "DELETE", "orgId=" + id);
        }
    }

    @GetMapping("/{orgId}/users")
    @PreAuthorize("hasAuthority('user:view')")
    public List<com.smile.usermanagement.entity.User> listOrgUsers(@PathVariable Long orgId) {
        return userService.listByOrgAndKeyword(orgId, null);
    }

    @PutMapping("/{orgId}/users/{userId}")
    @PreAuthorize("hasAuthority('user:edit')")
    public Map<String, Object> addUserToOrg(@PathVariable Long orgId, @PathVariable Long userId) {
        com.smile.usermanagement.entity.User user = userService.getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
        user.setOrgId(orgId);
        userService.updateById(user);
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        if (principal != null) {
            auditService.opLog(principal.id(), principal.username(), "ORG", "ADD_MEMBER", "orgId=" + orgId + ",userId=" + userId);
        }
        return Map.of("ok", true);
    }

    @DeleteMapping("/{orgId}/users/{userId}")
    @PreAuthorize("hasAuthority('user:edit')")
    public Map<String, Object> removeUserFromOrg(@PathVariable Long orgId, @PathVariable Long userId) {
        com.smile.usermanagement.entity.User user = userService.getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
        Org org = orgService.getById(orgId);
        Long fallbackOrgId = 1L;
        if (org != null && org.getParentId() != null && org.getParentId() > 0) {
            fallbackOrgId = org.getParentId();
        }
        user.setOrgId(fallbackOrgId);
        userService.updateById(user);
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        if (principal != null) {
            auditService.opLog(principal.id(), principal.username(), "ORG", "REMOVE_MEMBER",
                "orgId=" + orgId + ",userId=" + userId + ",toOrgId=" + fallbackOrgId);
        }
        return Map.of("ok", true);
    }
}
