package com.smile.usermanagement.controller;

import com.smile.usermanagement.dto.UserCreateRequest;
import com.smile.usermanagement.dto.UserUpdateRequest;
import com.smile.usermanagement.entity.User;
import com.smile.usermanagement.security.SecurityUtils;
import com.smile.usermanagement.security.UserPrincipal;
import com.smile.usermanagement.service.AuditService;
import com.smile.usermanagement.service.AuthService;
import com.smile.usermanagement.service.UserService;
import com.smile.usermanagement.web.PageResult;
import com.smile.usermanagement.web.ReplaceIdsRequest;
import jakarta.validation.Valid;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final AuditService auditService;

    public UserController(UserService userService, AuthService authService, AuditService auditService) {
        this.userService = userService;
        this.authService = authService;
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('user:view')")
    public List<User> listUsers(@RequestParam(required = false) Long orgId,
                                @RequestParam(required = false) String keyword) {
        return userService.listByOrgAndKeyword(orgId, keyword);
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('user:view')")
    public PageResult<User> pageUsers(@RequestParam(required = false) Long orgId,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(defaultValue = "1") Long page,
                                      @RequestParam(defaultValue = "20") Long size) {
        return PageResult.from(userService.pageByOrgAndKeyword(orgId, keyword, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:view')")
    public User getUser(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + id);
        }
        return user;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('user:add')")
    @ResponseStatus(HttpStatus.CREATED)
    public User createUser(@Valid @RequestBody UserCreateRequest request) {
        User user = new User();
        fillUser(user, request.getUsername(), request.getPassword(), request.getRealName(),
            request.getPhone(), request.getEmail(), request.getAvatar(), request.getOrgId(), request.getStatus());
        User created = userService.createUser(user, request.getRoleIds());
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        if (principal != null) {
            auditService.opLog(principal.id(), principal.username(), "USER", "CREATE", "userId=" + created.getId());
        }
        return created;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user:edit')")
    public User updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        User user = new User();
        fillUser(user, request.getUsername(), request.getPassword(), request.getRealName(),
            request.getPhone(), request.getEmail(), request.getAvatar(), request.getOrgId(), request.getStatus());
        User updated = userService.updateUser(id, user, request.getRoleIds());
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        if (principal != null) {
            auditService.opLog(principal.id(), principal.username(), "USER", "UPDATE", "userId=" + updated.getId());
        }
        return updated;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:delete')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id, @RequestHeader(value = "X-Second-Verify", required = false) String token) {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        if (principal != null) {
            authService.assertSecondVerified(principal.id(), token);
        }
        boolean removed = userService.deleteUserById(id);
        if (!removed) {
            throw new IllegalArgumentException("User not found: " + id);
        }
        if (principal != null) {
            auditService.opLog(principal.id(), principal.username(), "USER", "DELETE", "userId=" + id);
        }
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('user:edit')")
    public Map<String, Object> resetPassword(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String newPassword = body == null ? null : (String) body.get("newPassword");
        userService.resetPassword(id, newPassword);
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        if (principal != null) {
            auditService.opLog(principal.id(), principal.username(), "USER", "RESET_PASSWORD", "userId=" + id);
        }
        return Map.of("ok", true);
    }

    @GetMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('user:view')")
    public List<Long> listUserRoles(@PathVariable Long id) {
        return userService.listRoleIds(id);
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('role:assign')")
    public Map<String, Object> replaceUserRoles(@PathVariable Long id, @RequestBody ReplaceIdsRequest request) {
        userService.replaceRoles(id, request == null ? null : request.getIds());
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        if (principal != null) {
            auditService.opLog(principal.id(), principal.username(), "USER", "REPLACE_ROLES", "userId=" + id);
        }
        return Map.of("ok", true);
    }

    @GetMapping(value = "/export", produces = "text/csv")
    @PreAuthorize("hasAuthority('user:export')")
    public ResponseEntity<String> exportUsers(@RequestParam(required = false) Long orgId) {
        List<User> users = userService.listByOrgAndKeyword(orgId, null);
        StringBuilder sb = new StringBuilder();
        sb.append("id,username,realName,phone,email,avatar,orgId,status\n");
        for (User u : users) {
            sb.append(u.getId()).append(',')
                .append(csv(u.getUsername())).append(',')
                .append(csv(u.getRealName())).append(',')
                .append(csv(u.getPhone())).append(',')
                .append(csv(u.getEmail())).append(',')
                .append(csv(u.getAvatar())).append(',')
                .append(u.getOrgId()).append(',')
                .append(u.getStatus() == null ? 1 : u.getStatus())
                .append('\n');
        }
        return ResponseEntity.ok()
            .contentType(MediaType.valueOf("text/csv"))
            .body(sb.toString());
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('user:import')")
    public Map<String, Object> importUsers(@RequestPart("file") MultipartFile file) throws Exception {
        int created = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null) {
                throw new IllegalArgumentException("Empty CSV");
            }
            int offset = header.trim().toLowerCase().startsWith("id,") ? 1 : 0;
            String line;
            while ((line = reader.readLine()) != null) {
                if (!StringUtils.hasText(line)) {
                    continue;
                }
                String[] parts = line.split(",", -1);
                if (parts.length < offset + 8) {
                    continue;
                }
                User user = new User();
                user.setUsername(parts[offset].trim());
                user.setPassword(parts[offset + 1].trim()); // allow raw; service will encode
                user.setRealName(parts[offset + 2].trim());
                user.setPhone(parts[offset + 3].trim());
                user.setEmail(parts[offset + 4].trim());
                user.setAvatar(parts[offset + 5].trim());
                user.setOrgId(Long.parseLong(parts[offset + 6].trim()));
                user.setStatus(Integer.parseInt(parts[offset + 7].trim()));
                userService.createUser(user, null);
                created++;
            }
        }
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        if (principal != null) {
            auditService.opLog(principal.id(), principal.username(), "USER", "IMPORT", "count=" + created);
        }
        return Map.of("created", created);
    }

    private void fillUser(User user, String username, String password, String realName,
                          String phone, String email, String avatar, Long orgId, Integer status) {
        user.setUsername(username);
        user.setPassword(password);
        user.setRealName(realName);
        user.setPhone(phone);
        user.setEmail(email);
        user.setAvatar(avatar);
        user.setOrgId(orgId);
        user.setStatus(status);
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        String v = value.replace("\"", "\"\"");
        if (v.contains(",") || v.contains("\n") || v.contains("\r")) {
            return "\"" + v + "\"";
        }
        return v;
    }
}
